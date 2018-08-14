# maxgset-miner
This project contains code and data used to determine the candidate values for each cavity's new MaxGSET value.

Here is a high level outline of the aims of this software.  Much of the metadata reported is not terribly useful since it is a snapshot at the start of a gradient duration period.  Also note that an odd performance issue popped up when querying from the production 'ops' deployment that wasn't there when querying from 'opsfb'.

Here we want to determine what the maximum operationally stable gradient we have been able to acheive in a cavity.  This gives an indication of how much recoverable gradient we may have in a cavity.

Operationally stable here is defined as being able to be at an unchagned GSET value for a minimum of four hours where at least four of those hours have RF on and CW beam present with a trip rate less than one per two hours of RF On with CW Beam.

 - GSET value will be determined by the archived R???GSET PV
 - On/Off status of the RF system for a cavity will be determined by the archived R???ACK1.B6 PV
 - Beam presence determined by current reading in at least one of the Halls above stated threshold with Gun HV ON and at least one chopper slit not closed
   - Hall A BCM
     - PV: IBC1H04CRCUR2
     - Threshold: > 1 microA
   - Hall B BPM
     - PV: IPM2C24A.IENG
     - THreshold: > 1 nA
   - Hall C BCM
     - PV: IBC3H00CRCUR4
     - Threshold: > 1 microA
   - Hall D BCM
     - PV: IBCAD00CRCUR6
     - Threshold: > 1 nA
   - Gun HV 
     - Post 2018-06-06
       - PV: IGL0I00HV_On
       - 1 == On
     - Pre 2018-06-06
       - PV: IGL0I00HVONSTAT
       - 1 == On / enabled 

   - Chopper Slits 
     - 8 == completely closed/chopped for all PVs
     - Hall A PV: SMRPOSA
     - Hall B PV: SMRPOSB
     - Hall C PV: SMRPOSC
     - No dedicated Hall D PV
   - Alternative to Hall BCM, use the "beam present" PV field on BPM just
     - 2S01 BPM
       - PV: IPM2S01.BNSF
       - 0 == is present ???
   - Can this be validated by a PV from the RF system?
 - Beam mode is determined by Laser Mode enum PVs.  Beam considered to be in CW mode if master mode and at least one laser mode is set to CW Mode.  
   - CW Mode == 3 for all PVs
   - Laser A Mode PV: IGL1I00HALLAMODE
   - Laser B Mode PV: IGL1I00HALLBMODE
   - Laser C Mode PV: IGL1I00HALLCMODE
   - Laser D Mode PV: IGL1I00HALLDMODE
     - Not present before 2016-08-19.

Additional Metadata to be saved for each MaxGSET value
 - Measure of Beam loaing: R2XXITOT
 - Per Hall Pass Setting
   - Hall A PV: MMSHLAPASS
   - Hall B PV: MMSHLBPASS
   - Hall C PV: MMSHLCPASS
   - Hall D PV: MMSHLDPASS
 - FaultAnalyzer trip data during GSET period
   - For each trip:
     - time
     - type
     - FaultAnalyzer System Status (System Ready, Maintenance, Testing)
 - Per Hall Beam current
   - Hall A PV: IBC1H04CRCUR2
   - Hall B PV: IPM2C24A.IENG
   - Hall C PV: IBC3H00CRCUR4
   - Hall D PV: IBCAD00CRCUR64545


min_duration = 4 hours
max_trip_rate = 0.5 / hour

foreach cavity
  search_start_date = <something>
  max = 0
  foreach gset_segment between search_start_date and now
    if duration of gset_segment > min_duration
      if summed duration of cavity_rf_on on  during gset_segment > min_duration
        if summed duration of beam present during cavity rf_on > min_duration
          if cavity_trip_rate < max_trip_rate
            max = gset_value of gset_segment
          end if
        end if
      end if
    end if
  end foreach gset_segment
  cavity_MaxGset = max
end foreach cavity
