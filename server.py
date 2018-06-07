# -*- coding: utf-8 -*-
from flask import flash, Flask, jsonify,request, url_for, send_from_directory,redirect #FLASK is class and jsonify=method
from flask_pymongo import PyMongo
from werkzeug.utils import secure_filename
import logging ,os,io
import csv
import numpy as np
import scipy as sp
#import matplotlib.pyplot as plt
from scipy import signal
import pandas as pd
from pandas import DataFrame
from scipy.signal import find_peaks_cwt
import sys
from numpy import NaN, Inf, arange, isscalar, asarray, array
import pdb
from math import factorial
import pymongo
from pymongo import MongoClient
from flask_mail import Mail,Message
import json
app=Flask(__name__)
mail=Mail(app)


DOWNLOAD_FOLDER='/home/ec2-user/server-flask-template/server'
UPLOAD_FOLDER = '/home/ec2-user/server-flask-template/server'
ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif','dat','csv'])

#testing
app.config['MONGO_DBNAME']='stressmonitoring'
app.config['MONGO_URI']='mongodb://username:password@ds137720.mlab.com:37720/stressmonitoring'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['DOWNLOAD_FOLDER'] = DOWNLOAD_FOLDER
app.config['MAIL_SERVER']='smtp.gmail.com'
app.config['MAIL_PORT']=465
app.config['MAIL_USE_SSL']=True
app.config['MAIL_USERNAME']='mailid@gmail.com'
app.config['MAIL_PASSWORD']='password'
mail=Mail(app)
mongo=PyMongo(app)

def allowed_file(filename):
    return '.' in filename and            filename.rsplit('.', 1)[1] in ALLOWED_EXTENSIONS
@app.route('/', methods=['GET', 'POST'])
def upload_file():
    if request.method == 'POST':
        print('hello')
        # check if the post request has the file part
        if 'file' not in request.files:
            print('No file part')
            return redirect(request.url)
        file = request.files['file']

        if file.filename == '':
            print('No selected file')
            return redirect(request.url)
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
            GSR_skin_resist_cal_lst=[]
            GSR_ppg_cal_lst=[]
            with open(filename) as f:
                reader = csv.reader(f, delimiter=',')

                #STORING DATA IN LISTS
                for line in reader:
                    GSR_skin_resist_cal_lst.append(line[2])
                    GSR_ppg_cal_lst.append(line[1])
            f.close()

            #CUTTING LISTS
            GSR_skin_resist_cal_lst=GSR_skin_resist_cal_lst[200:]
            GSR_ppg_cal_lst=GSR_ppg_cal_lst[20:]
            #CHANGING FORMAT FROM STRING TO FLOAT
            GSR_skin_resist_cal_lst = [float (i) for i in GSR_skin_resist_cal_lst]
            GSR_ppg_cal_lst=[float (i) for i in GSR_ppg_cal_lst]
            #PLOTTING RAW PPG VALUES
            #plt.plot(GSR_ppg_cal_lst)
            #plt.title('Raw PPG')

            #FILTERING GSR DATA USING MEDIAN FILTER
            kernel_size = 21
            sample_frequency = 128
            filtered_GSR_skin_resist_cal_lst = sp.signal.medfilt(GSR_skin_resist_cal_lst,kernel_size)

            #plt.plot(filtered_GSR_skin_resist_cal_lst, 'r')
            #plt.title('filtered GSR')

            def mean(data):
                n = 0
                mean = 0.0

                for x in data:
                    n += 1
                    mean += (x - mean)/n

                if n < 1:
                    return float('nan');
                    #PPG DATA FILTERING USING MEDIAN FILTER
                   sample_frequency = 128
                   kernel_size = 51

                   filtered1_GSR_ppg_cal_lst = sp.signal.medfilt(GSR_ppg_cal_lst,kernel_size)
                   meanv= mean(filtered1_GSR_ppg_cal_lst)
                   for i in range (0, len(filtered1_GSR_ppg_cal_lst)) :
                       if filtered1_GSR_ppg_cal_lst[i]<=800 or filtered1_GSR_ppg_cal_lst[i]>=2000:
                           filtered1_GSR_ppg_cal_lst[i]=meanv
                   #plt.plot(filtered1_GSR_ppg_cal_lst)
                   #plt.title('Median Filtered PPG')

                   #PPG DATA - BUTTERWORTH FILTERING
                   N = 1
                   sample_frequency = 128
                   fc = 1
                   Wn = fc/sample_frequency #Normalized Frequency
                   B, A = signal.butter(N, Wn, output='ba')
                   smooth_data = signal.filtfilt(B,A, filtered1_GSR_ppg_cal_lst)
                   #plt.plot(smooth_data)
                   #plt.title('buterworth filtered PPG')

                   #GSR DATA - MOVING AVERAGE
                   sample_frequency=128
                   rates = np.array([40,80,100,150,200])/60
                   df=pd.DataFrame(filtered_GSR_skin_resist_cal_lst)
                   moving_GSR_skin_resist_cal_lst=df.rolling(window=sample_frequency*60, min_periods=1).mean()
                   #plt.plot(moving_GSR_skin_resist_cal_lst, 'r')
                   #plt.title('Aggregation GSR')

                   #DEFINING PEAK DETECTION FUNCTION
                   def peakdet(v, delta, x = None):

                           maxtab = []
                           mintab = []

                           if x is None:
                               x = arange(len(v))

                           v = asarray(v)

                           if len(v) != len(x):
                               sys.exit('Input vectors v and x must have same length')

                           if not isscalar(delta):
                               sys.exit('Input argument delta must be a scalar')

                           if delta <= 0:
                               sys.exit('Input argument delta must be positive')
                               mn, mx = Inf, -Inf
                        mnpos, mxpos = NaN, NaN

                        lookformax = True

                        for i in arange(len(v)):
                            this = v[i]
                            if this > mx:
                                mx = this
                                mxpos = x[i]
                            if this < mn:
                                mn = this
                                mnpos = x[i]

                            if lookformax:
                                if this < mx-delta:
                                    maxtab.append((mxpos, mx))
                                    mn = this
                                    mnpos = x[i]
                                    lookformax = False
                            else:
                                if this > mn+delta:
                                    mintab.append((mnpos, mn))
                                    mx = this
                                    mxpos = x[i]
                                    lookformax = True

                        return array(maxtab), array(mintab)

                #GSR - STRESS FINAL PLOT
                #from matplotlib.pyplot import plot, scatter, show
                series = moving_GSR_skin_resist_cal_lst
                maxtab, mintab = peakdet(series,1)
                #series1=maxtab[:,0]
                #figGSR=plt.figure()
                #plt.plot(series,'g')

                #PPG - PRV FINAL PLOT
                import random
                maxtab,mintab=peakdet(smooth_data,1)
                trial=maxtab[:,0]
                peakIntervalHR = np.diff(trial);
                HR = 60.*sample_frequency/(peakIntervalHR);
                meanHR = mean(HR)
                for i in range(0, len(HR)):
                    if HR[i]<=60 or HR[i]>150:
                        HR[i] = random.randint(80,95)
                for i in range (0,len(HR)-1):
                    if HR[i] - HR[i+1] >20:
                        HR[i]= HR[i]*0.7 + HR[i+1]*0.3
                    if HR[i+1] - HR[i] > 20 :
                    HR[i]= HR[i]*0.7 + HR[i+1]*0.3

         print ("------------HEART RATE VARIABILITY IS ----------")
         VARIABILITY = np.var(HR)
         print (" Variability is " + str(VARIABILITY))
         with open('file1.csv','w') as f1:
             np.savetxt(f1,HR,delimiter=',',fmt='%f')
         with open('file2.csv','w') as f2:
             np.savetxt(f2,series,delimiter=',',fmt='%f')
         #with open('file3.csv','w') as f3:
             #np.savetxt(f3,VARIABILITY,delimiter=',',fmt='%f')
         #h=pd.read_csv("hr.csv")
         #g=pd.read_csv("gsr.csv")
         pieces=[]
         for num in [1,2]:
             s=pd.read_csv('file%d.csv'%num)
             pieces.append(s)
         #pieces.append(VARIABILITY)
         newcsv=pd.concat(pieces,axis=1)
         newcsv.to_csv("output.csv")
        # json_hr=json.dumps(HR)
        # now = new Date()
         userhr=mongo.db.hr
         usergsr=mongo.db.gsr
         for i in range(0,len(HR)-1):
             userhr.insert({'value-hr':HR[i]})
         #for j in range(0,len(series1)-1):
             #usergsr.insert({'value-gsr':series[j]})
         #for i in range(0,len(HR)-1):
             #userhr.insert({'value-hr':HR[i]})
             #userhr.update({'time':now})
             #return 'abcd'
         #str1="Stress Data HR variability",VARIABILITY
         msg=Message(subject="Stress Result", sender=app.config.get("MAIL_USERNAME"),recipients=["mailid2@gmail.com"])
         msg.body="Stress Monitor Analysis result Variability: "+str(VARIABILITY)
         mail.send(msg)
         return send_from_directory(app.config['UPLOAD_FOLDER'],filename="output.csv")

 return jsonify({})



@app.route('/downloads', methods=['GET', 'POST'])
def downloaded_file():
 resp = send_from_directory(app.config['DOWNLOAD_FOLDER'], filename="plot.png")
 return resp

 @app.route('/uploads/<filename>')
 def uploaded_file(filename):
     return send_from_directory(app.config['UPLOAD_FOLDER'],filename)

 #LISTENING AND BINDING TO CLIENT
 if __name__ == '__main__':
     app.debug=True
     app.run(host='0.0.0.0', port=8080,threaded=True)
