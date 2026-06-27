ServerOptions.devices();

(
s.options.inDevice_("MacBook Pro Microphone");
s.options.outDevice_("External Headphones");
s.newBusAllocators;
~bus = Bus.audio(s, 2);
s.options.memSize_(2.pow(20));
s.reboot;
)

// Microsound - reconstructing a signal from grains
// Starting with loading in sample

~projectRoot = "/Users/dmanz/Desktop/kernos";

~buf = Buffer.readChannel(s, ~projectRoot +/+ "samples/poem_natural_clean.wav", channels: [0]);
~buf.query;
~buf.play;
(
Ndef(\granular_reconstruct).clear;
Ndef(\granular_reconstruct).ar(2);
Ndef(\granular_reconstruct).set(\bufnum, ~buf.bufnum);
)

(
Ndef(\granular_reconstruct, { |bufnum, overlap=2, tFreq=20|
	var phasor, bufrd, gran, env; 
	var bufFrames = BufFrames.ir(bufnum);
	var t = Impulse.ar(tFreq);

	phasor = Phasor.ar(
		rate: 1.0, 
		start: 0.0, 
		end: bufFrames, 
	);

	bufrd = BufRd.ar(
		numChannels: 1, 
		bufnum: bufnum, 
		phase: phasor, 
		interpolation: 0
	);

	gran = GrainBuf.ar(
		numChannels: 1, 
		trigger: t, 
		dur: overlap / tFreq, 
		sndbuf: bufnum, 
		rate: 1, 
		pos: phasor / bufFrames, 
		interp: 0, 
		pan: 0, 
		envbufnum: -1, 
		maxGrains: 512, 
	);


	// bufrd
	// - 
	gran
	!2
}).play;
)

Spec.add(\overlap, [0.001, 40, \exp]);
Ndef(\granular_reconstruct).gui;
Spec.add(\tFreq, \widefreq);

// env
e = Buffer.sendCollection(s, Signal.hanningWindow(1024));
e.plot

( // playing it at right rate to fit duration

var dur=0.4;
{
	PlayBuf.ar(1, e.bufnum, rate: 1 / ( dur * s.sampleRate / e.numFrames ), doneAction: 2);
}.plot(dur)

)