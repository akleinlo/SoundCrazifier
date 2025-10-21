;=============================================
; Test Score for granular.orc
;=============================================

; f1 = Audiofile (Mono oder Stereo WAV)
f1 0 1048576 1 "/Users/adriankleinlosen/Desktop/tryout.wav" 0 0 0

; optional: Sine-Wave-Test (nicht nötig, da im .orc erzeugt)
; f2 0 8192 10 1

;---------------------------------------------
; i1 p1=0s, p3=20s Dauer
; p4–p26 sind Parameter, die du nach Belieben variieren kannst
;---------------------------------------------

i1 0 20 \
  0.1 0.5  2 6 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
  0.5 2    2 6 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
  2 10     2 6 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
  0.0 0.2  2 6 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
  -0.2 0.2 2 6 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
  0.005 0.1 2 6         ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax
e
