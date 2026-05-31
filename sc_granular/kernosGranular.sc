s.options.outDevice = "BlackHole 16ch";
s.options.numInputBusChannels = 0;  // disable input completely, no mismatch possible
s.options.sampleRate = 48000;       // match BlackHole's native rate
s.boot;

MIDIClient.init;
MIDIIn.connectAll;

~projectRoot = "/Users/dmanz/Desktop/kernos";

// Reading in sample as mono
~buf = Buffer.readChannel(s, ~projectRoot +/+ "samples/whisper.mp3", channels: [0]);
~buf.query;

(
    var env = Env.new([0, 1, 0], [0.01, 1], [0, -4]);
    ~grainEnv = Buffer.loadCollection(s, env.discretize(8192));
)
(
SynthDef(\kernosGranular, {
    arg buf, grainEnv, trigRate = 30, grainPitch = 7, pitchSpread = 0.1, panSpread = 0.2, dur = 0.5, posSpread = 0.4;
    var triggerSignal = Impulse.kr(trigRate);
    var grainPos = LFSaw.kr(1 / BufDur.ir(buf)).range(0, 1) + TRand.kr(0 - posSpread, posSpread, triggerSignal);
    var grainRate = (grainPitch + TRand.kr(0 - pitchSpread, pitchSpread, triggerSignal)).midiratio();
    var pan = TRand.kr(0 - panSpread, panSpread, triggerSignal);
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
    Out.ar(0, sig ! 2);
}).add;
)



(
x = Synth(\kernosGranular, [
    \buf, ~buf,
    \grainEnv, ~grainEnv,
    \trigRate: 20,
    \grainPitch: -7,
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


// Dry signal, if needed
(
SynthDef(\samplePlayer, {
    // Specifying run-time arguments
    arg buf, amp = 1, attack = 0.1;
    var sig, env;

    sig = PlayBuf.ar(1, buf, doneAction: 2);
    env = Line.kr(0, 1, attack);
    Out.ar(0, (sig * env * amp) ! 2);

}).add;
)

(
y = Synth(\samplePlayer, [
    \buf, ~buf,
    \envBuf, ~grainEnv
]);
)