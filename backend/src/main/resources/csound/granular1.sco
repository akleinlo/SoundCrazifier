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

;----------------- CRAZIFICATION LEVEL 1 -----------------
i1  0.0       REPLACE_ME_DURATION \
    0.5       0.5     1.0   2.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    1.0       1.0     1.0   2.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    2.0       2.0     1.0   2.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.0     1.0   2.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    0.0       0.0     1.0   2.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.5    	  0.5     1.0   2.0 \        ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -20.00   20.0	  0.5	1.5 \	     ; iazLo, iazHi, iazMinCps, iazMaxCps
    -9.00 	 9.0	  0.5	1.5	         ; ielLo, ielHi, ielMinCps, ielMaxCps

