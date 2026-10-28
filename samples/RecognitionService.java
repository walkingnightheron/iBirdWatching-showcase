package com.shaojiawen.ibirdwatchingserver.service;

import ai.onnxruntime.OrtException;
import com.shaojiawen.ibirdwatchingserver.common.BizException;
import com.shaojiawen.ibirdwatchingserver.common.ErrorCode;
import com.shaojiawen.ibirdwatchingserver.inference.BirdClassifier;
import com.shaojiawen.ibirdwatchingserver.inference.ImageUtils;
import com.shaojiawen.ibirdwatchingserver.inference.OrtSessionManager;
import com.shaojiawen.ibirdwatchingserver.inference.YoloDetector;
import com.shaojiawen.ibirdwatchingserver.model.dto.RecognitionResponse;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

@Service
public class RecognitionService {

    private static final double CROP_PAD = 0.1;
    private static final double LOW_CONFIDENCE_THRESHOLD = 0.35;

    private final YoloDetector yoloDetector;
    private final BirdClassifier birdClassifier;

    public RecognitionService(YoloDetector yoloDetector, BirdClassifier birdClassifier) {
        this.yoloDetector = yoloDetector;
        this.birdClassifier = birdClassifier;
    }

    /**
     * 识别管线：YOLO 检测全部鸟 → 逐框裁切 → EfficientNet 分类 Top-5。
     */
    public RecognitionResponse recognize(BufferedImage img) {
        List<YoloDetector.Detection> detections;
        try {
            detections = yoloDetector.detectBirds(img);
        } catch (OrtException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "识别服务异常");
        }

        List<YoloDetector.Detection> birds = detections.stream()
                .filter(d -> d.cls() == YoloDetector.BIRD_CLASS)
                .toList();

        if (birds.isEmpty()) {
            throw new BizException(ErrorCode.DETECTION_FAILED);
        }

        List<RecognitionResponse.Detection> result = birds.stream()
                .map(bird -> buildDetection(img, bird))
                .toList();

        return new RecognitionResponse(result);
    }

    private RecognitionResponse.Detection buildDetection(BufferedImage img, YoloDetector.Detection bird) {
        ImageUtils.Letterboxed lb = ImageUtils.letterbox(img, OrtSessionManager.YOLO_INPUT_SIZE);
        double[] orig = ImageUtils.toOriginal(bird.box()[0], bird.box()[1], bird.box()[2], bird.box()[3], lb, CROP_PAD);
        BufferedImage crop = crop(img, orig);

        List<BirdClassifier.Candidate> top;
        try {
            top = birdClassifier.topK(crop, 5);
        } catch (OrtException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "识别服务异常");
        }

        List<RecognitionResponse.Candidate> candidates = top.stream()
                .map(c -> new RecognitionResponse.Candidate(c.rank(), c.species(), c.nameZh(), c.confidence()))
                .toList();

        double maxConf = candidates.isEmpty() ? 0 : candidates.get(0).confidence();
        RecognitionResponse.BirdBox box = new RecognitionResponse.BirdBox(
                orig[0], orig[1], orig[2] - orig[0], orig[3] - orig[1]);

        return new RecognitionResponse.Detection(
                box, bird.confidence(), candidates, maxConf < LOW_CONFIDENCE_THRESHOLD);
    }

    private BufferedImage crop(BufferedImage img, double[] box) {
        int x = (int) Math.floor(box[0]);
        int y = (int) Math.floor(box[1]);
        int w = (int) Math.ceil(box[2]) - x;
        int h = (int) Math.ceil(box[3]) - y;
        w = Math.min(w, img.getWidth() - x);
        h = Math.min(h, img.getHeight() - y);
        if (w <= 0 || h <= 0) {
            throw new BizException(ErrorCode.DETECTION_FAILED);
        }
        BufferedImage sub = img.getSubimage(x, y, w, h);
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(sub, 0, 0, null);
        g.dispose();
        return copy;
    }
}
