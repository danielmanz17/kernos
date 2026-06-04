s.boot;

(
OSCdef(\fluxHello, {
    arg msg, time, addr, recvPort;
    ("Flux received: " ++ msg[1]).postln;
    },
'/flux'
);
)
