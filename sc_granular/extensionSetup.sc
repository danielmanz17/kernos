s.boot;

// Print SC extensions folder
Platform.userExtensionDir();

// De-qaurantine extension
NN.deQuarantine;
s.reboot;

~projectRoot = "/Users/dmanz/Desktop/kernos";
~modelPath = ~projectRoot +/+ "models/water.ts";
~samplePath = ~projectRoot +/+ "samples/angel.wav";

// Load model
NN.load(\water, ~modelPath);
// List available methods
NN(\water).methods;

b = Buffer.read(s, ~samplePath);
{ NN(\vintage, \forward).ar(PlayBuf.ar(1, b, loop: 1)) }.play;

