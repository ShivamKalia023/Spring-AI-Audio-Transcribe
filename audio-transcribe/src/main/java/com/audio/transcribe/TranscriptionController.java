package com.audio.transcribe;

import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class TranscriptionController {

    private OpenAiAudioTranscriptionModel transcriptionModel;
    private  CommandHandler commandHandler;

    public TranscriptionController(OpenAiAudioTranscriptionModel transcriptionModel, CommandHandler commandHandler) {
        this.transcriptionModel = transcriptionModel;
        this.commandHandler=commandHandler;
    }
    @PostMapping("transcribe")
    public String speechToText(@RequestParam MultipartFile file){
        String text = transcriptionModel.call(file.getResource());
        commandHandler.handleTranscription(text);
        return text;
    }
}
