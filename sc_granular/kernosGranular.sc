s.options.outDevice = "BlackHole 16ch";
s.options.numInputBusChannels = 0;  // disable input completely, no mismatch possible
s.options.sampleRate = 48000;       // match BlackHole's native rate
s.boot;

MIDIClient.init;
MIDIIn.connectAll;

~projectRoot = "/Users/dmanz/Desktop/kernos";

// Reading in sample as mono
~buf = Buffer.readChannel(s, ~projectRoot +/+ "samples/poem_natural.wav", channels: [0]);
~buf.query;

(
    var env = Env.new([0, 1, 0], [0.01, 1], [0, -4]);
    ~grainEnv = Buffer.loadCollection(s, env.discretize(8192));
)
(
SynthDef(\kernosGranular, {
    // Defining args, can be set during run-time
    arg buf, grainEnv, trigRate = 30, grainPitch = 7, pitchSpread = 0.1, panSpread = 0.2, dur = 0.5, posSpread = 0.4, dryWet = 1;

    // Defining vars
    var triggerSignal = Impulse.kr(trigRate);
    var grainPos = LFSaw.kr(1 / BufDur.ir(buf)).range(0, 1) + TRand.kr(0 - posSpread, posSpread, triggerSignal);
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
    dry = PlayBuf.ar(2, buf, loop: 1);
    dry = dry ! 2;

    // Mixing sample and granular signal branches
    mix = XFade2.ar(dry, sig, dryWet * 2 - 1);

    Out.ar(0, mix);
}).add;
)



(
x = Synth(\kernosGranular, [
    \buf, ~buf,
    \grainEnv, ~grainEnv,
    \trigRate: 20,
    \grainPitch: 7,
    \pitchSpread: 0.01,
    \panSpread: 0.01,
    \dur: 0.5,
    \posSpread: 0.3
])
)

// Midi mapping
// Encoder 1 to modulate pitch spread
(
    MIDIdef.cc(\pitchSpreadControl, { 
        arg val, num, chan, src;
        var pitchSpread = val.linexp(0, 127, 0.1, 30.0);
        x.set(\pitchSpread, pitchSpread);
    }, ccNum: 10);
)

// Encoder 2 to modulate trigger rate
(
    MIDIdef.cc(\trigControl, { 
        arg val, num, chan, src;
        var trigRate = val.linexp(0, 127, 0.1, 200);
        x.set(\trigRate, trigRate)
    }, ccNum: 74);
)

// Encoder 3 to modulate grain length
(
    MIDIdef.cc(\durControl, { 
        arg val, num, chan, src;
        var dur = val.linexp(0, 127, 0.01, 4.0);
        x.set(\dur, dur);
    }, ccNum: 71);
)

// Encoder 4 to modulate grain pos spread
(
    MIDIdef.cc(\posControl, { 
        arg val, num, chan, src;
        var posSpread = val.linexp(0, 127, 0.01, 2.0);
        x.set(\posSpread, posSpread);
    }, ccNum: 76);
)

// Encoder 5 to modulate central pitch
(
MIDIdef.cc(\pitch, { 
    arg val, num, chan, src;

    var pitch = val.linlin(0, 127, -12, 12); // semitones

    x.set(\grainPitch, pitch);
}, ccNum: 77);
)

// Encoder 6 to modulate dry/wet between sample/granular branch
(
MIDIdef.cc(\pitch, { 
    arg val, num, chan, src;

    var dryWet = val.linlin(0, 127, -1.0, 1.0); // semitones
    x.set(\dryWet, dryWetx);

}, ccNum: 93);
)
