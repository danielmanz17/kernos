ServerOptions.devices();

s.options.inDevice_("MacBook Pro Microphone");
s.options.outDevice_("MacBook Pro Speakers");
s.boot;

// The default input/output busses corresponding to hardware
// These are "public busses"
s.options.numOutputBusChannels();
s.options.numInputBusChannels();

s.options.numAudioBusChannels();
s.options.numControlBusChannels();

// Set to default if needed
(
    s.options.numOutputBusChannels_(2);
    s.options.numInputBusChannels_(2);
    s.reboot;
)

// Examples of creating busses to send signals
(   
    s.newBusAllocators();
    ~mixerBus = Bus.audio(s, 8);
    ~delayBus = Bus.audio(s, 2);
    ~reverbBus = Bus.audio(s, 2);
)

// Example of writing an audio signal to bus index 14 & 15

(
x = {Out.ar(14, SinOsc.ar([150, 151], mul: 0.2))}.play;
s.scope(rate: \audio, numChannels: 2, index: 14);
)

// Using a bus to pass a signal from one audio module to another
(
    s.newBusAllocators();
    ~bus = Bus.audio(s, 2);
    
    SynthDef(\src, {
        arg out = 0, freq = 150;
        var sig = SinOsc.ar(freq + [0, 1]);
        Out.ar(out, sig);
    }).add;

    SynthDef(\fader, {
        arg in = 0, out = 0;
        var sig = In.ar(in, 2);
        sig = sig * 0.2;
        Out.ar(out, sig);
    }).add;

)


(
    ~fader = Synth(\fader, [in: ~bus, out: 0]);
    ~src = Synth(\src, [out: ~bus]);
)

s.scope(rate: \audio, numChannels: 16, index: 0);

// Two independent signals/synths, observing node tree
s.plotTree();

(
    SynthDef(\sine, {
        arg gate = 1, out = 0;
        var sig = SinOsc.ar(250, mul: 0.1);
        sig = sig * Env.asr.kr(2, gate);
        Out.ar(out, sig);
    }).add;
)

(
    SynthDef(\pink, {
        arg gate = 1, out = 0;
        var sig = PinkNoise.ar(0.1 ! 2);
        sig = sig * Env.asr.kr(2, gate);
        Out.ar(out, sig);
    }).add;
)

~a = Synth(\sine);
~b = Synth(\pink);

s.plotTree();

(
    s.newBusAllocators();
    ~bus = Bus.audio(s, 2);
)

(
SynthDef(\pulses, {
    arg out = 0, freq = 800, pulseFreq = 0.3;
    var sig = SinOsc.ar(freq + [0, 2], mul: 0.2);
    sig = sig * LFPulse.kr(pulseFreq, 0, 0.01);
    Out.ar(out, sig);
}).add;
)

(
    SynthDef(\reverb, {
        arg in = 0, out = 0, size = 0.97;
        var sig = In.ar(in, 2);
        sig = FreeVerb2.ar(sig[0], sig[1], mix: 0.6, room: size);
        Out.ar(out, sig);
    }).add;
)

~a = Synth(\pulses);
~b = Synth(\reverb);

s.plotTree();

// Hello world delay example, burst of noise
(
x = {
        var sig, delay;
        Line.kr(0, 0, 1, doneAction: 2);
        sig = PinkNoise.ar(0.5 ! 2) * XLine.kr(1, 0.001, 0.3);
        delay = DelayN.ar(sig, 0.5, 0.5) * -6.dbamp;
        sig = sig + delay;
    }
)
x.play(fadeTime: 0);

// Seprating source and delay into two modules

(
    s.newBusAllocators();
    ~bus = Bus.audio(s, 2);

    SynthDef(\del, {
        arg in = 0, out = 0, delTime = 0.5, amp = 0.5;
        var drySig, delSig, sig;
        drySig = In.ar(in, 2);
        delSig = DelayN.ar(drySig, 1, delTime) * amp;
        sig = drySig + delSig;
        Out.ar(out, sig);
    }).add;

    SynthDef(\src, {
        arg out = 0;
        var sig = PinkNoise.ar(0.5 ! 2);
        sig = sig * XLine.kr(1, 0.001, 0.3, doneAction: 2);
        Out.ar(out, sig);
    }).add;

)

s.plotTree;

~del = Synth(\del, [in: ~bus, out: 0]);
~src = Synth(\src, [out: ~bus]);

// Variable time delay for flanger 

(
    {
        var sig, delay, lfo;
        lfo = SinOsc.kr(0.1, 3pi/2).range(0.001, 0.01);
        sig = LPF.ar(Saw.ar(100 ! 2, mul: 0.2), 2000);
        delay = DelayL.ar(sig, 0.01, lfo);
        sig = sig + delay;
    }.play;
)

// Simple comb delay for feedback
(
    {
        var sig, delay;
        Line.kr(0, 0, 10, doneAction: 2);
        sig = PinkNoise.ar(0.5 ! 2) * XLine.kr(1, 0.001, 0.3);
        delay = CombN.ar(sig, 0.1, 0.1, 3) * -6.dbamp();
        sig = sig + delay;
    }.play(fadeTime: 0);
)

// Variable comb for resonant behaviour 
(
    SynthDef(\comb, {
        arg freq = 4;
        var sig, delay;
        Line.kr(0, 0, 10, doneAction: 2);
        sig = PinkNoise.ar(0.5 ! 2) * XLine.kr(1, 0.001, 0.3);
        delay = CombN.ar(sig, 1, 1/freq, 9) * -6.dbamp;
        delay = LeakDC.ar(delay);
        sig = sig + delay;
        Out.ar(0, sig);
    }).add;
)

Synth(\comb, [freq: 30]);