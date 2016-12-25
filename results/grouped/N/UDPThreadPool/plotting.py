import numpy as np
import pandas as pd
import matplotlib.pylab as plt
import os

folder = "grouped"


params = ['M', 'N', 'Delta']
metrics = ['RequestTime', 'ClientProcTime', 'ClientWorkTime']

for param in params:
    for metric in metrics:
        title = ""
        for res in os.listdir(os.path.join(folder, param)):
            pathValues = os.path.join(folder, param, res, "ParamsValues.csv")
            with open(pathValues, 'r') as f:
                title = " ".join(f.readlines()[:3])
            path = os.path.join(folder, param, res, "benchmarkResults.csv")
            df = pd.read_csv(path, skiprows=1, sep=";")
            plt.plot(df['ParamValue'], df[metric])
        plt.title(title)
        plt.show()