from flask import flash, Flask, jsonify,request, url_for, send_from_directory,redirect #FLASK is class and jsonify=method
#for file upload testing
from werkzeug.utils import secure_filename
import logging ,os
app=Flask(__name__)


UPLOAD_FOLDER = '/Users/tintujose/Downloads/uploads'
ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif','dat','csv'])

#testing tintujose

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER



def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1] in ALLOWED_EXTENSIONS
@app.route('/', methods=['GET', 'POST'])
def upload_file():
    if request.method == 'POST':
        # check if the post request has the file part
        if 'file' not in request.files:
            print('No file part')
            return redirect(request.url)
        file = request.files['file']
        # if user does not select file, browser also
        # submit a empty part without filename
        if file.filename == '':
            print('No selected file')
            return redirect(request.url)
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
            return redirect(url_for('uploaded_file',filename=filename))


    #request_data=request.get_json()

    #resp = Response("Uploaded", status=201, mimetype='application/json')
    return jsonify({})
@app.route('/uploads/<filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'],filename)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080)
