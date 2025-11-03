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

/*
;----------------- CRAZIFICATION LEVEL 2 -----------------
i2  0	      10.0 \
    0.45      0.55     1.25   2.5 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.99      1.01     1.25   2.5  \       ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    1.5       2.5      1.25   2.5  \       ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.075    1.25   2.5  \       ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.075    0.075    1.25   2.5  \       ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.4       0.5      1.25   2.5          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -40.00    40.0	   0.6	  2.0		   ; iazLo, iazHi, iazMinCps, iazMaxCps
    -18.00 	  18.0	   0.6	  2.0		   ; ielLo, ielHi, ielMinCps, ielMaxCps


;----------------- CRAZIFICATION LEVEL 3 -----------------
i3  0	      10.0 \
    0.4       0.5      1.5   3.0 \         ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.975     1.025    1.5   3.0 \         ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    1.25      3.0      1.5   3.0 \         ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.1	   1.5   3.0 \         ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.1      0.1      1.5   3.0 \         ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.3       0.5      1.5   3.0           ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -60.00 	  60.0	   0.7	 2.5		   ; iazLo, iazHi, iazMinCps, iazMaxCps
    -27.00 	  27.0	   0.7	 2.5		   ; ielLo, ielHi, ielMinCps, ielMaxCps


;----------------- CRAZIFICATION LEVEL 4 -----------------
i4  0	      10.0 \
    0.35      0.5     1.75   3.5 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.95      1.05    1.75   3.5 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    1.0       5.0     1.75   3.5 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.125   1.75   3.5 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.125    0.125   1.75   3.5 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.2       0.5     1.75   3.5          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -80.00 	  80.0	  0.8	 3.0		  ; iazLo, iazHi, iazMinCps, iazMaxCps
    -36.00 	  36.0	  0.8	 3.0		  ; ielLo, ielHi, ielMinCps, ielMaxCps


;----------------- CRAZIFICATION LEVEL 5 -----------------
i5  0	      10.0 \
    0.3       0.5     2.0   4.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.925     1.075   2.0   4.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    0.8       7.0     2.0   4.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.15    2.0   4.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.15     0.15    2.0   4.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.1       0.5     2.0   4.0          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -100.0 	  100.0	  0.9	3.5			 ; iazLo, iazHi, iazMinCps, iazMaxCps
    -45.00 	  45.0	  0.9	3.5			 ; ielLo, ielHi, ielMinCps, ielMaxCps


;----------------- CRAZIFICATION LEVEL 6 -----------------
i6  0	      10.0 \
    0.25      0.5     2.25   5.5 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.9       1.1     2.25   5.5 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    0.7       9.0     2.25   5.5 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.175   2.25   5.5 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.175    0.175   2.25   5.5 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.05      0.5     2.25   5.5          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -120.0 	  120.0	  1.0	 5.0		  ; iazLo, iazHi, iazMinCps, iazMaxCps
    -54.00 	  54.0	  1.0	 5.0		  ; ielLo, ielHi, ielMinCps, ielMaxCps


;----------------- CRAZIFICATION LEVEL 7 -----------------
i7  0	      10.0 \
    0.2       0.5     2.5   7.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.8       1.2     2.5   7.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    0.6       12.0    2.5   7.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.2     2.5   7.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.2      0.2     2.5   7.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.025     0.45    2.5   7.0          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -150.0 	  150.0	  1.1	7.0			 ; iazLo, iazHi, iazMinCps, iazMaxCps
    -63.00 	  63.0	  1.1	7.0			 ; ielLo, ielHi, ielMinCps, ielMaxCps


;----------------- CRAZIFICATION LEVEL 8 -----------------
i8  0	      10.0 \
    0.1       0.5     2.75   9.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.6       1.4     2.75   9.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    0.5       15.0    2.75   9.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.225   2.75   9.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.225    0.225   2.75   9.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.01      0.4     2.75   9.0          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -180.0 	  180.0	  1.2	 9.5		  ; iazLo, iazHi, iazMinCps, iazMaxCps
    -72.00 	  72.0	  1.2	 9.5		  ; ielLo, ielHi, ielMinCps, ielMaxCps


;----------------- CRAZIFICATION LEVEL 9 -----------------
i9  0	      10.0 \
    0.05      0.5     3.0   12.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.4       2.0     3.0   12.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    1.0       19.0    3.0   12.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.25    3.0   12.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.25     0.25    3.0   12.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.005     0.35    3.0   12.0          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -210.00   210.0	  1.3	12.5	      ; iazLo, iazHi, iazMinCps, iazMaxCps
    -81.00 	  81.0	  1.3	12.5		  ; ielLo, ielHi, ielMinCps, ielMaxCps


;----------------- CRAZIFICATION LEVEL 10 -----------------
i10 0         10.0 \
    0.025     0.6     3.25   16.5 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.3       3.0     3.25   16.5 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    2.0       23.0    3.25   16.5 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.275   3.25   16.5 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.275    0.275   3.25   16.5 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.0005    0.1     3.25   16.5          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -250.0 	  250.0	  1.4	 16.0		   ; iazLo, iazHi, iazMinCps, iazMaxCps
    -90.00 	  90.0	  1.4	 16.0		   ; ielLo, ielHi, ielMinCps, ielMaxCps
*/