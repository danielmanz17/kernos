s.quit;
ServerOptions.devices();

// Set input/output devices
(
s.options.inDevice_("MacBook Pro Microphone");
s.options.outDevice_("MacBook Pro Speakers");
s.boot;
)

// Assigning busses for peripheral flux and grain aux
(
s.newBusAllocators();
~xBus = Bus.control(s, 1);
~yBus = Bus.control(s, 1);
~grainBus = Bus.audio(s, 2);
)

// Assigning buffer for live GRAINBUF
(
Buffer.freeAll;
~grainBuf = Buffer.alloc(s, s.sampleRate * 3);
)

// Global variables for incoming flux
(
OSCdef(\flux, {
    
    arg msg;
    ~xBus.set(msg[1]);
    ~yBus.set(msg[1]);

}, '/flux');
)

// Print SC extensions folder
Platform.userExtensionDir();

// De-qaurantine extension
NN.deQuarantine;
s.reboot;

// Defining paths
(
~projectRoot = "/Users/dmanz/Desktop/kernos";
~modelPath = ~projectRoot +/+ "models/voice_vctk_b2048_r44100_z22.ts";
~samplePath = ~projectRoot +/+ "samples/poem_tts.wav";
)

// Load sample to buffer
~buf = Buffer.readChannel(s, ~samplePath, channels: [0]);

// Load model
NN.load(\voice, ~modelPath);
// List available methods
NN(\voice).methods;

// Server.default.record;

// Granular module
(
SynthDef(\grainGen, {
	arg in = 0, gate = 1, atk = 0.01, rel = 3, cf = 20000,
	del = 0.25, dens = 30, graindur = 0.07, pan = 0, amp = 1, out = 0;
	var sig, env, panctrl;
	sig = In.ar(in, 1);
	// for now, using an ASR envelope
	env = EnvGen.kr(
		Env.asr(atk, 1, rel, -2),
		gate, doneAction: 2
	);
	// random interpolation between + and - pan
	panctrl = LFNoise1.kr(20).bipolar(pan);
	// Applying lpf before delay and granulation
	sig = LPF.ar(sig, cf);
	sig = DelayN.ar(sig, del, del);
	// Dust is random impulse generator	
	sig = GrainIn.ar(2, Dust.kr(dens), graindur, sig, panctrl);
	sig = sig * env * amp;
	Out.ar(out, sig);
}).add;
)

// NN audio synthesis module
(
SynthDef(\nnVoice, {
    arg out = 0, auxOut = 0, amp = 1, auxAmp = 0;
    var latent, offsetLatent, sig, x_offset, y_offset;
    var latentOffset = Array.fill(22, 0);

    x_offset = In.kr(~xBus);
    y_offset = In.kr(~yBus);

    // assign first two dims
    latentOffset[2] = x_offset;
    latentOffset[4] = y_offset;

    // latentOffset.do {
    //     arg l, i;
    //     l.poll(1, ("latent offset inde x " ++ i).asSymbol);
    // };

    latent = NN(\voice, \encode).ar(
        PlayBuf.ar(1, ~buf, loop: 1)
    );

    offsetLatent = latent.collect { arg l, i;
        l + latentOffset[i]
    };
    
    sig = NN(\voice, \decode).ar(offsetLatent);
    Out.ar(out, sig ! 2 * amp);
	Out.ar(auxOut, sig * auxAmp);
}).add;
)

(
~grains = Synth(\grain, [
	in: ~grainBus,
	atk: 1,
	rel: 5,
	cf: 5000,
	del: 0.15,
	pan: 0.7,
]);
~voice = Synth(\nnVoice, [auxOut: ~grainBus, auxAmp: 0.7]);
)

s.scope(rate: \audio, numChannels: 2, index: 4);
s.scope(rate: \audio, numChannels: 2, index: 0);
s.plotTree;