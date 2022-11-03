import scipy
from scipy.io import wavfile
  
# function to convert the information into 
# some readable format
def output_duration(length):
    return length * 1000
  
# sample_rate holds the sample rate of the wav file
# in (sample/sec) format
# data is the numpy array that consists
# of actual data read from the wav file
while True:
    filename = input()

    try:
        sample_rate, data = wavfile.read(filename + '.wav')
        
        len_data = len(data)  # holds length of the numpy array
        
        t = len_data / sample_rate  # returns duration but in floats
        
        duration = output_duration(t)
        print('Total Duration: {}'.format(duration))
    except Exception as e:
        print("error...")
        print(e)