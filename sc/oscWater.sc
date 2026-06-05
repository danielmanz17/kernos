s.boot;

(
~fluxBus = Bus.control(s, 1);
)

// (
// OSCdef(\flux, {
//     arg msg;

//     // msg[1] is your flux (0–5)
//     ~fluxBus.set(msg[1]);

// }, '/flux');
// )

OSCdef(\flux, {
    
    arg msg;

    var a, b;

    a = msg[1]; // first value
    b = msg[2]; // second value

    ("A: " ++ a ++ "  B: " ++ b).postln;

}, '/flux');

(
SynthDef(\waterTone, {
    var flux, freq, sig;

    // read control signal
    flux = In.kr(~fluxBus);

    // scale 0–5 → frequency range
    freq = flux.linlin(0, 5, 100, 1000);

    sig = SinOsc.ar(freq) * 0.2;

    Out.ar(0, sig ! 2);
}).add;
)

x = Synth(\waterTone);