ServerOptions.devices();

s.options.inDevice_("MacBook Pro Microphone");
s.options.outDevice_("BlackHole 16ch");
s.boot;

// black hole routing
// s.options.outDevice = "BlackHole 16ch";
// s.options.numInputBusChannels = 0;  // disable input completely, no mismatch possible
// s.options.sampleRate = 48000;       // match BlackHole's native rate
// s.boot;

MIDIClient.init;
MIDIIn.connectAll;

~projectRoot = "/Users/dmanz/Desktop/kernos";

// Reading in sample as mono
~buf = Buffer.readChannel(s, ~projectRoot +/+ "samples/rave_out.wav", channels: [0]);
~buf.query;

(
    var env = Env.new([0, 1, 0], [0.01, 1], [0, -4]);
    ~attackEnv = Buffer.loadCollection(s, env.discretize(8192));
)

(
    var env = Env.sine();
    ~sineEnv = Buffer.loadCollection(s, env.discretize(8192));
)

(
var env = Env([0, 1, 0], [0.25, 0.25], \sine);
~gaussEnv = Buffer.loadCollection(s, env.discretize(8192));
)

(
var env = Env.perc(0.001, 0.05, curve: -4);
~percEnv = Buffer.loadCollection(s, env.discretize(8192));
)

(
var env = Env([0, 1, 0], [0.8, 0.2], [4, -4]);
~revEnv = Buffer.loadCollection(s, env.discretize(8192));
)

(
var env = Env([0, 1, 0, 1, 0], [0.15, 0.15, 0.15, 0.15]);
~doubleEnv = Buffer.loadCollection(s, env.discretize(8192));
)

(
var env = Env([0, 1, 0], [0.001, 0.001]);
~clickEnv = Buffer.loadCollection(s, env.discretize(8192));
)
(
SynthDef(\kernosGranular, {
    // Defining args, can be set during run-time
    arg buf, grainEnv, trigRate = 30, grainPitch = 0, pitchSpread = 0, panSpread = 0, dur = 0.08, posSpread = 0, dryWet = 1;

    // Defining vars
    var triggerSignal = Impulse.kr(trigRate);
    var phase = Phasor.ar(trig: 0, rate: BufRateScale.ir(buf), start: 0, end: BufFrames.ir(buf)) / BufFrames.ir(buf);   
    var grainPos = phase + TRand.kr(0 - posSpread, posSpread, triggerSignal);
    var grainRate = (grainPitch + TRand.kr(0 - pitchSpread, pitchSpread, triggerSignal)).midiratio;
    var pan = TRand.kr(0 - panSpread, panSpread, triggerSignal);
    var dry, mix;

    // Generating granular signal branch
    var sig = GrainBuf.ar(
        numChannels: 2,
        trigger: triggerSignal,
        dur: dur,
        sndbuf: buf,
        rate: grainRate,
        pos: grainPos,
        interp: 2,
        pan: pan,
        envbufnum: grainEnv
    );

    // Generating dry signal branch
    dry = PlayBuf.ar(1, buf, loop: 1);
    dry = dry ! 2;

    // Mixing sample and granular signal branches
    mix = XFade2.ar(dry, sig, dryWet * 2 - 1);

    Out.ar(0, mix);

}).add;
)

(
x = Synth(\kernosGranular, [
    \buf, ~buf,
    \grainEnv, ~sineEnv
])
)

x.set(\trigRate, 8);
x.set(\dur, 0.3);
x.set(\posSpread, 0.8);
(
~kernosPresets = (
    natural: (
        trigRate: 30,
        grainPitch: 0,
        pitchSpread: 0,
        panSpread: 0,
        dur: 0.08,
        posSpread: 0,
        dryWet: 1
    )
)
)

// Midi mapping

// Enc. 1, modulate trigger control
(
    MIDIdef.cc(\triggerCtrl, { 
        arg val, num, chan, src;
        var trigRate = val.linexp(0, 127, 0.1, 200);
        x.set(\trigRate, trigRate)
    }, ccNum: 10);
)
// Enc. 2, modulate central grain pitch
(
    MIDIdef.cc(\pitchCtrl, { 
        arg val, num, chan, src;
        var pitch = val.linlin(0, 127, -12, 12); // semitones
        x.set(\grainPitch, pitch);
    }, ccNum: 74);
)

// Enc. 3, modulate pitch spread
(
    MIDIdef.cc(\pitchSpreadCtrl, { 
        arg val, num, chan, src;
        var pitchSpread = val.linexp(0, 127, 0.1, 30.0);
        x.set(\pitchSpread, pitchSpread);
    }, ccNum: 71);
)

// Enc. 4, modulate grain duration
(
    MIDIdef.cc(\durCtrl, { 
        arg val, num, chan, src;
        var dur = val.linexp(0, 127, 0.01, 4.0);
        x.set(\dur, dur);
    }, ccNum: 76);
)

// Enc. 5, modulate grain start spread

(
MIDIdef.cc(\posCtrl, { 
    arg val, num, chan, src;
    var posSpread = val.linexp(0, 127, 0.01, 2.0);
    x.set(\posSpread, posSpread);
}, ccNum: 77);
)

// Enc. 6, modulate dry/wet
(
MIDIdef.cc(\pitch, { 
    arg val, num, chan, src;
    var dryWet = val.linlin(0, 127, -1.0, 1.0); // semitones
    x.set(\dryWet, dryWet);
}, ccNum: 93);
)

// Seq. 1, natural preset
(
MIDIdef.cc(\naturalPreset, {
    arg val, num, chan, src;
    if(val == 127) {
        x.set(*~kernosPresets[\natural].asKeyValuePairs);
    };

}, ccNum: 20);
)


x.set(\grainEnv, ~gaussEnv);


