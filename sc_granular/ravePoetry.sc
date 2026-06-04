ServerOptions.devices();

s.options.inDevice_("MacBook Pro Microphone");
s.options.outDevice_("BlackHole 16ch");

s.boot;

// Global variable for incoming flux
(
~xBus = Bus.control(s, 1);
~yBus = Bus.control(s, 1);
)

OSCdef(\flux, {
    
    arg msg;
    ~xBus.set(msg[1]);
    ~yBus.set(msg[1]);

}, '/flux');

// Print SC extensions folder
Platform.userExtensionDir();

// De-qaurantine extension
NN.deQuarantine;
s.reboot;

~projectRoot = "/Users/dmanz/Desktop/kernos";
~modelPath = ~projectRoot +/+ "models/voice_vctk_b2048_r44100_z22.ts";
~samplePath = ~projectRoot +/+ "samples/poem_tts.wav";

// Load sample to buffer
~buf = Buffer.readChannel(s, ~samplePath, channels: [0]);

// Load model
NN.load(\voice, ~modelPath);
// List available methods
NN(\voice).methods;

// Server.default.record;

// Encoded latent (sample) offset
(
{
    var latent, offsetLatent, sig, x_offset, y_offset, latentOffset;

    latentOffset = Array.fill(22, 0);

    x_offset = In.kr(~xBus);
    y_offset = In.kr(~yBus);

    // assign first two dims
    latentOffset[2] = x_offset;
    latentOffset[4] = y_offset ;

    // latentOffset.do {
    //     arg l, i;
    //     l.poll(1, ("latent offset index " ++ i).asSymbol);
    // };

    latent = NN(\voice, \encode).ar(
        PlayBuf.ar(1, ~buf, loop: 1)
    );

    offsetLatent = latent.collect { arg l, i;
        l + latentOffset[i]
    };

    sig = NN(\voice, \decode).ar(offsetLatent);
    sig ! 2;
}.play;
)
