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

;----------------- CRAZIFICATION LEVEL 3 -----------------
i1  0	      REPLACE_ME_DURATION \
    0.4       0.5      1.5   3.0 \         ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.975     1.025    1.5   3.0 \         ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    1.25      3.0      1.5   3.0 \         ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.1	   1.5   3.0 \         ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.1      0.1      1.5   3.0 \         ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.3       0.5      1.5   3.0           ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -60.00 	  60.0	   0.7	 2.5		   ; iazLo, iazHi, iazMinCps, iazMaxCps
    -27.00 	  27.0	   0.7	 2.5		   ; ielLo, ielHi, ielMinCps, ielMaxCps

