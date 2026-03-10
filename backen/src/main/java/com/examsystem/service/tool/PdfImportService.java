package com.examsystem.service.tool;

import com.examsystem.common.ImageUtils;
import com.examsystem.config.AppProperties;
import com.examsystem.entity.Image;
import com.examsystem.entity.Option;
import com.examsystem.entity.Question;
import com.examsystem.entity.Stem;
import com.examsystem.mapper.ImageMapper;
import com.examsystem.mapper.StemMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

@Service
@Slf4j
public class PdfImportService {

    @Autowired
    private ImageMapper imageMapper;
    
    @Autowired
    private StemMapper stemMapper;
    
    @Autowired
    private AppProperties appProperties;

    public List<Question> parsePdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            // Extract images first
            Map<String, String> imageMap = extractImages(document);
            // Extract text
            String text = extractText(document);
            // Parse questions with image references
            return parseQuestionsFromText(text, imageMap);
        }
    }

    private String extractText(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        return stripper.getText(document);
    }

    private Map<String, String> extractImages(PDDocument document) throws IOException {
        Map<String, String> imageMap = new HashMap<>();
        int imageCounter = 1;
        String baseDir = System.getProperty("user.dir") + "/src/main/resources/static";

        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            for (COSName name : resources.getXObjectNames()) {
                PDXObject xobject = resources.getXObject(name);
                if (xobject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xobject;
                    BufferedImage bufferedImage = image.getImage();
                    
                    // Log image found
                    log.info("Found image in PDF: width={}, height={}, format={}", 
                            bufferedImage.getWidth(), 
                            bufferedImage.getHeight(), 
                            image.getSuffix());
                    
                    // Convert BufferedImage to MultipartFile
                    MultipartFile imageFile = convertToMultipartFile(bufferedImage, "image" + imageCounter + ".png");
                    
                    // Save image and get relative path
                    String relativePath = ImageUtils.saveImage(imageFile, baseDir, 1L); // Using 1L as default user ID
                    
                    // Save to database
                    Image imageEntity = new Image();
                    imageEntity.setName(relativePath.substring(relativePath.lastIndexOf('/') + 1));
                    imageEntity.setPath(relativePath);
                    imageEntity.setType("image/png");
                    imageEntity.setSize(imageFile.getSize());
                    imageEntity.setCreatedTime(LocalDateTime.now());
                    imageMapper.insert(imageEntity);
                    
                    // Log image saved
                    log.info("Image saved: id={}, path={}", imageEntity.getId(), relativePath);
                    
                    // Store in map with a placeholder key
                    String imageUrl = appProperties.getBaseUrl() + "/api/images/" + imageEntity.getId();
                    imageMap.put("[IMAGE" + imageCounter + "]", imageUrl);
                    imageCounter++;
                }
            }
        }

        return imageMap;
    }

    private MultipartFile convertToMultipartFile(BufferedImage image, String fileName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        
        return new MultipartFile() {
            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public boolean isEmpty() {
                return bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return bytes;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                try (java.io.OutputStream os = new java.io.FileOutputStream(dest)) {
                    os.write(bytes);
                }
            }
        };
    }

    private List<Question> parseQuestionsFromText(String text, Map<String, String> imageMap) {
        List<Question> questions = new ArrayList<>();
        
        // Convert imageMap to a list for sequential access
        List<String> imageUrls = new ArrayList<>(imageMap.values());
        int imageIndex = 0;

        String[] lines = text.split("\\r?\\n");
        Question currentQuestion = null;
        List<Option> currentOptions = new ArrayList<>();
        Stem currentStem = null;
        boolean inStem = false;
        
        // Patterns
        Pattern questionStartPattern = Pattern.compile("^\\d+\\.\\s*(.*)");
        Pattern optionPattern = Pattern.compile("^([A-D])\\.\\s*(.*)");
        Pattern answerPattern = Pattern.compile("^(?:答案|Answer)[:：]\\s*([A-D]).*", Pattern.CASE_INSENSITIVE);
        Pattern analysisPattern = Pattern.compile("^(?:解析|Analysis)[:：]\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Pattern stemStartPattern = Pattern.compile("^(?:资料分析|材料|材料分析)[:：]?\\s*(.*)", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Check if this is the start of a stem (资料分析)
            Matcher stemMatcher = stemStartPattern.matcher(line);
            if (stemMatcher.find()) {
                // Save previous question if exists
                if (currentQuestion != null) {
                    // Fill empty options with images
                    for (Option option : currentOptions) {
                        if (option.getValue() == null || option.getValue().trim().isEmpty()) {
                            if (imageIndex < imageUrls.size()) {
                                option.setImageUrl(imageUrls.get(imageIndex));
                                log.info("Filled empty option {} with image: {}", option.getKey(), imageUrls.get(imageIndex));
                                imageIndex++;
                            }
                        }
                    }
                    
                    if (!currentOptions.isEmpty()) {
                        currentQuestion.setOptions(new ArrayList<>(currentOptions));
                    }
                    questions.add(currentQuestion);
                }
                
                // Start new stem
                currentStem = new Stem();
                currentStem.setContent(stemMatcher.group(1));
                currentStem.setCategory("资料分析");
                currentStem.setCreatedTime(LocalDateTime.now());
                currentStem.setCreateBy(1L);
                inStem = true;
                currentQuestion = null;
                currentOptions = new ArrayList<>();
                continue;
            }

            // If we're in a stem, collect stem content
            if (inStem && currentStem != null) {
                // Check if this line starts a question
                Matcher qMatcher = questionStartPattern.matcher(line);
                if (qMatcher.find()) {
                    // Save the stem to database
                    stemMapper.insert(currentStem);
                    log.info("Created stem with id: {}", currentStem.getId());
                    
                    // Check if there are images for the stem
                    if (imageIndex < imageUrls.size()) {
                        currentStem.setContentImageUrl(imageUrls.get(imageIndex));
                        stemMapper.updateById(currentStem);
                        log.info("Added image to stem: {}", imageUrls.get(imageIndex));
                        imageIndex++;
                    }
                    
                    // Start new question with stemId
                    currentQuestion = new Question();
                    currentQuestion.setContent(qMatcher.group(1));
                    currentQuestion.setType(1); // Default to Single Choice (1) for now
                    currentQuestion.setDifficulty(3); // Default difficulty
                    currentQuestion.setStemId(currentStem.getId());
                    currentQuestion.setCategory("资料分析");
                    currentOptions = new ArrayList<>();
                    inStem = false;
                    continue;
                } else {
                    // Append to stem content
                    currentStem.setContent(currentStem.getContent() + "\n" + line);
                    continue;
                }
            }

            Matcher qMatcher = questionStartPattern.matcher(line);
            if (qMatcher.find()) {
                // Save previous question if exists
                if (currentQuestion != null) {
                    // Fill empty options with images
                    for (Option option : currentOptions) {
                        if (option.getValue() == null || option.getValue().trim().isEmpty()) {
                            if (imageIndex < imageUrls.size()) {
                                option.setImageUrl(imageUrls.get(imageIndex));
                                log.info("Filled empty option {} with image: {}", option.getKey(), imageUrls.get(imageIndex));
                                imageIndex++;
                            }
                        }
                    }
                    
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
            // Fill empty options with images
            for (Option option : currentOptions) {
                if (option.getValue() == null || option.getValue().trim().isEmpty()) {
                    if (imageIndex < imageUrls.size()) {
                        option.setImageUrl(imageUrls.get(imageIndex));
                        log.info("Filled empty option {} with image: {}", option.getKey(), imageUrls.get(imageIndex));
                        imageIndex++;
                    }
                }
            }
            
            if (!currentOptions.isEmpty()) {
                currentQuestion.setOptions(new ArrayList<>(currentOptions));
            }
            questions.add(currentQuestion);
        }

        // Log remaining images
        if (imageIndex < imageUrls.size()) {
            log.info("Remaining images not used: {}", imageUrls.size() - imageIndex);
        }

        return questions;
    }
}
