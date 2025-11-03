;=============================================
; Test Score for granular.orc
;=============================================

; f1 = Audiofile (Mono or Stereo WAV)
f1 0 0 1 "REPLACE_ME" 0 0 0

; optional: Sine-Wave-Test (not necessary, as it is generated in the .orc file.)
; f2 0 8192 10 1

;---------------------------------------------
; i1=instrument, p2=start time, p3=duration
; p4–p34 are parameters that you can be varied
;---------------------------------------------

;----------------- CRAZIFICATION LEVEL 10 -----------------
i1  0         REPLACE_ME_DURATION \
    0.025     0.6     3.25   16.5 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.3       3.0     3.25   16.5 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    2.0       23.0    3.25   16.5 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.275   3.25   16.5 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.275    0.275   3.25   16.5 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.0005    0.1     3.25   16.5          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -250.0 	  250.0	  1.4	 16.0		   ; iazLo, iazHi, iazMinCps, iazMaxCps
    -90.00 	  90.0	  1.4	 16.0		   ; ielLo, ielHi, ielMinCps, ielMaxCps
