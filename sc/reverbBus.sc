ServerOptions.devices();

s.options.inDevice("MacBook Pro Microphone");
s.options.outDevice("MacBook Pro Speakers");
s.boot;

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

    SynthDef(\fader, {
        arg in = 0, out = 0;
        var sig = In.ar(in, 2);
        sig = sig * 0.2;
        Out.ar(out, sig);
    }).add;

    SynthDef(\src, {
        arg out = 0, freq = 150;
        var sig = SinOsc.ar(freq + [0, 1]);
        Out.ar(out, sig);
    }).add;
)


(
    ~fader = Synth(\fader, [in: ~bus, out: 0]);
    ~src = Synth(\src, [out: ~bus]);
)

s.scope(rate: \audio, numChannels: 16, index: 0);