#!/usr/bin/python

import argparse
import os
import pandas as pd
import subprocess

# -----------------------------------------------------------------------------------------------------
# SUPPORT FUNCTIONS
# -----------------------------------------------------------------------------------------------------

# This method scans the log file and returns the last value corresponding to the header
# e.g. header   = quantity to extract (must match name of Star-CCM+ monitor e.g. CD, CL, or CM)
#      filename = name of log file (e.g. star.log)
def parse_value(filename, header):
    col = None
    value = None
    residual_names = "Iteration     Continuity     X-momentum     Y-momentum         Energy"
    with open(filename) as f:
        for line in f:
            if residual_names in line and header in line:
                headers = line.split()
                col = headers.index(header)
            if col:
                try:
                    values = line.split()
                    value = float(values[col])
                except IndexError:
                    continue
                except ValueError:
                    continue
    return value

# -----------------------------------------------------------------------------------------------------
# COMMAND LINE ARGUMENTS
# -----------------------------------------------------------------------------------------------------

# Get command line arguments
parser = argparse.ArgumentParser(description='This program runs the specified Star-CCM+ simulation')

parser.add_argument('-hpc', action="store_true", dest="isCluster",
                    default=False,
                    help='True if program is running on a cluster')

parser.add_argument('-jar', action="store", dest="class_path", type=str,
                    default=None,
                    help='Path of the jar file containing the user-defined classes')

parser.add_argument('-macro', action="store", dest="macro", type=str,
                    default=os.getcwd(),
                    help='Path of the java macro')

parser.add_argument('-sim', action="store", dest="sim_file", type=str,
                    default=None,
                    help='Path of Star-CCM+ simulation file')

parser.add_argument('-doe', action="store", dest="doe_file", type=str,
                    default=None,
                    help='Path of the csv file containing the DOE')

parser.add_argument('-N', action="store", dest="number_processors", type=str,
                    default="2",
                    help='Number of processors eg. $NPROCS')

parser.add_argument("-pwd", action="store", dest="work_dir", type=str,
                    default=os.getcwd(),
                    help='Present working directory eg. $PBS_O_WORKDIR')

parser.add_argument("-nodes", action="store", dest="machinefile", type=str,
                    default=None,
                    help='Cluster machine file eg. $PBS_NODEFILE')
					
parser.add_argument("-pod", action="store", dest="podkey", type=str,
                    default=None,
                    help='License power on demand key')		
					
parser.add_argument("-lic", action="store", dest="licencepath", type=str,
                    default="1999@flex.cd-adapco.com",
                    help='License server')	

args = parser.parse_args()

# -----------------------------------------------------------------------------------------------------
# MAIN PROGRAM
# -----------------------------------------------------------------------------------------------------

# Extract doe
doe = pd.read_csv(args.doe_file)
number_cases = doe.shape[0]

# Create columns for the outputs
doe["CL"] = None
doe["CD"] = None
doe["CM"] = None

# Initialize dictionary
inputs = dict({"jar": args.class_path,
               "sim": args.sim_file,
               "np": args.number_processors,
               "pod": args.podkey,
               "lic": args.licencepath,
               "macro": args.macro,
               "hpc": args.isCluster,
               "pwd": args.work_dir,
               "nodes": args.machinefile,
               "Pref": None,
               "dP": None,
               "rho": None,
               "T": None,
               "M": None,
               "mu": None,
               "a": None,
               "alpha": None,
               "save": None,
               "id": None})

# Loop over DOE
for i in range(0, number_cases):

    # Extract values for next doe case
    inputs["id"] = doe['id'].iloc[i]
    inputs["Pref"] = doe['Pref'].iloc[i]
    inputs["dP"] = doe['dP'].iloc[i]
    inputs["M"] = doe['M'].iloc[i]
    inputs["mu"] = doe['mu'].iloc[i]
    inputs["rho"] = doe['rho'].iloc[i]
    inputs["T"] = doe['T'].iloc[i]
    inputs["a"] = doe['a'].iloc[i]
    inputs["alpha"] = doe['alpha'].iloc[i]
    inputs["save"] = "case_" + str(inputs["id"]) + ".sim"

    # Display case separator
    print("*************************")
    if inputs["id"] < 10:
        print("*** Running Case " + str(inputs["id"]) + " ******")
    elif inputs["id"] < 100:
        print("*** Running Case " + str(inputs["id"]) + " *****")
    elif inputs["id"] < 1000:
        print("*** Running Case " + str(inputs["id"]) + " ****")
    else:
        print("*** Running Case " + str(inputs["id"]) + " ***")
    print("*************************")

    # Setup system call
    if inputs["hpc"]:
        sys_call = 'starccm+ ' \
                   '-podkey {pod} ' \
                   '-licpath {lic} ' \
                   '-mpi intel ' \
                   '-power ' \
                   '-np {np} ' \
                   '-classpath {jar} ' \
                   '-machinefile {nodes} ' \
                   '-jvmargs -Dalpha={alpha} ' \
                   '-jvmargs -DPref={Pref} ' \
                   '-jvmargs -DdP={dP} ' \
                   '-jvmargs -DM={M} ' \
                   '-jvmargs -Drho={rho} ' \
                   '-jvmargs -DT={T} ' \
                   '-jvmargs -Dmu={mu} ' \
                   '-jvmargs -Da={a} ' \
                   '-jvmargs -Dsave={save} ' \
                   '-batch {macro} {sim} ' \
                   '> case_{id}.log'.format(**inputs)
    elif inputs["jar"]:
        sys_call = 'starccm+ ' \
                   '-cpubind ' \
                   '-rsh ssh ' \
                   '-podkey {pod} ' \
                   '-licpath {lic} ' \
                   '-power ' \
                   '-np {np} ' \
                   '-classpath {jar} ' \
                   '-jvmargs -Dalpha={alpha} ' \
                   '-jvmargs -DPref={Pref} ' \
                   '-jvmargs -DdP={dP} ' \
                   '-jvmargs -DM={M} ' \
                   '-jvmargs -Drho={rho} ' \
                   '-jvmargs -DT={T} ' \
                   '-jvmargs -Dmu={mu} ' \
                   '-jvmargs -Da={a} ' \
                   '-jvmargs -Dsave={save} ' \
                   '-batch {macro} {sim} ' \
                   '> case_{id}.log'.format(**inputs)
    else:
        sys_call = 'starccm+ ' \
                   '-cpubind ' \
                   '-rsh ssh ' \
                   '-podkey {pod} ' \
                   '-licpath {lic} ' \
                   '-power ' \
                   '-np {np} ' \
                   '-jvmargs -Dalpha={alpha} ' \
                   '-jvmargs -DPref={Pref} ' \
                   '-jvmargs -DdP={dP} ' \
                   '-jvmargs -DM={M} ' \
                   '-jvmargs -Drho={rho} ' \
                   '-jvmargs -DT={T} ' \
                   '-jvmargs -Dmu={mu} ' \
                   '-jvmargs -Da={a} ' \
                   '-jvmargs -Dsave={save} ' \
                   '-batch {macro} {sim} ' \
                   '> case_{id}.log'.format(**inputs)
    print(sys_call)

    # Run Star-CCM+
    subprocess.run(sys_call, shell=True)

    # Post-process: extract CD, CL, CM and write to file
    CL = parse_value('case_{id}.log'.format(**inputs), "CL")
    CD = parse_value('case_{id}.log'.format(**inputs), "CD")
    CM = parse_value('case_{id}.log'.format(**inputs), "CM")

    # Update doe
    doe.loc[i, 'CL'] = CL
    doe.loc[i, 'CD'] = CD
    doe.loc[i, 'CM'] = CM

# Write doe to file
doe.to_csv(args.doe_file, index=False)
