package com.examsystem.service.tool;

import com.examsystem.entity.Option;
import com.examsystem.entity.Question;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PdfImportService {

    public List<Question> parsePdf(MultipartFile file) throws IOException {
        String text = extractText(file);
        return parseQuestionsFromText(text);
    }

    private String extractText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private List<Question> parseQuestionsFromText(String text) {
        List<Question> questions = new ArrayList<>();
        // Split text by lines to process line by line or use regex on full text
        // Improved Regex strategy:
        // Look for pattern starting with Number + Dot (e.g. "1.")
        
        // Pattern explanation:
        // Start: ^\d+\.\s*(.*)  -> Capture question number and content
        // Options: [A-D]\.\s*(.*) -> Capture options
        // Answer: 答案[：:]\s*([A-D])
        // Analysis: 解析[：:]\s*(.*)
        
        // Since PDF extraction might not preserve perfect line breaks, we need to be careful.
        // We will assume standard formatting where each part is on a new line or clearly separated.

        String[] lines = text.split("\\r?\\n");
        Question currentQuestion = null;
        List<Option> currentOptions = new ArrayList<>();
        
        // Patterns
        Pattern questionStartPattern = Pattern.compile("^\\d+\\.\\s*(.*)");
        Pattern optionPattern = Pattern.compile("^([A-D])\\.\\s*(.*)");
        Pattern answerPattern = Pattern.compile("^(?:答案|Answer)[:：]\\s*([A-D]).*", Pattern.CASE_INSENSITIVE);
        Pattern analysisPattern = Pattern.compile("^(?:解析|Analysis)[:：]\\s*(.*)", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher qMatcher = questionStartPattern.matcher(line);
            if (qMatcher.find()) {
                // Save previous question if exists
                if (currentQuestion != null) {
                    if (!currentOptions.isEmpty()) {
                        currentQuestion.setOptions(new ArrayList<>(currentOptions));
                    }
                    questions.add(currentQuestion);
                }
                
                // Start new question
                currentQuestion = new Question();
                currentQuestion.setContent(qMatcher.group(1));
                currentQuestion.setType(1); // Default to Single Choice (1) for now
                currentQuestion.setDifficulty(3); // Default difficulty
                currentOptions = new ArrayList<>();
                continue;
            }

            if (currentQuestion != null) {
                Matcher optMatcher = optionPattern.matcher(line);
                if (optMatcher.find()) {
                    Option option = new Option();
                    option.setKey(optMatcher.group(1));
                    option.setValue(optMatcher.group(2));
                    currentOptions.add(option);
                    continue;
                }

                Matcher ansMatcher = answerPattern.matcher(line);
                if (ansMatcher.find()) {
                    currentQuestion.setAnswer(ansMatcher.group(1));
                    continue;
                }

                Matcher anaMatcher = analysisPattern.matcher(line);
                if (anaMatcher.find()) {
                    currentQuestion.setAnalysis(anaMatcher.group(1));
                    continue;
                }
                
                // If line doesn't match any pattern, append to last field?
                // For simplicity, maybe append to question content if options/answer haven't started?
                if (currentOptions.isEmpty() && currentQuestion.getAnswer() == null) {
                     currentQuestion.setContent(currentQuestion.getContent() + "\n" + line);
                } else if (currentQuestion.getAnalysis() != null) {
                    currentQuestion.setAnalysis(currentQuestion.getAnalysis() + "\n" + line);
                }
            }
        }
        
        // Add last question
        if (currentQuestion != null) {
            if (!currentOptions.isEmpty()) {
                currentQuestion.setOptions(new ArrayList<>(currentOptions));
            }
            questions.add(currentQuestion);
        }

        return questions;
    }
}
