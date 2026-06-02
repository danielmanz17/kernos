ServerOptions.devices();

s.options.inDevice("MacBook Pro Microphone");
s.options.outDevice("External Headphones");
s.boot;

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

// Baseline resynthesis using forward/sample playback
(
    {
        var sig = NN(\voice, \forward).ar(PlayBuf.ar(1, ~buf, loop: 1));
        sig = sig ! 2;
    }.play;
)

// Encoded latent (sample) offset
(
{
    var latent, offsetLatent, sig, x, y, latentOffset;

    // x = left/right, y = up/down
    x = MouseX.kr(-5, 5);
    y = MouseY.kr(5, -5);

    latentOffset = Array.fill(22, 0);

    // assign first two dims
    latentOffset[0] = x;
    latentOffset[1] = y;

    latentOffset.do {
        arg l, i;
        l.poll(1, ("latent offset index " ++ i).asSymbol);
    };

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


