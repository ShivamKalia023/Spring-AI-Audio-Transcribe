import { useState } from "react";
import axios from 'axios';

const AudioUploader =()=>{

    const [file,setFile] = useState(null);
    const [transcription,settranscription]=useState("");

    const handleFileChange=(e)=>{

        setFile(e.target.files[0]);

    };

    const handleUploader= async()=>{
        const formData=new FormData();
        formData.append('file',file);

        try{
            const response= await axios.post('http://localhost:8080/api/transcribe',formData,{
                headers:{
                    'Content-Type':'multipart/form-data',

                }
            });
            settranscription(response.data);
        }catch(error){

            console.error("Error Transcribing Audio",error)

        }
    };


    return (
        <div className="container">
            <h1> Audio to Text Transcriber </h1>
            <div className="file-input">
                <input type="file" accept="audio/*" onChange={handleFileChange}/>
            </div>
            <button className="upload-button" onClick={handleUploader}>Upload and Transcribe</button>

            <div className="transcription-result">
                <h2>Transcription Result</h2>
                <p>{transcription} </p>
            </div>

        </div>
    )

}

export default AudioUploader;