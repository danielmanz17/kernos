s.boot;

MIDIClient.init;
MIDIIn.connectAll;

MIDIFunc.trace(true);
MIDIFunc.trace(false);

(
MIDIdef.cc(\filter74, { |val, num, chan, src|
    ("Knob 74 value: " ++ val).postln;
}, ccNum: 74);
)