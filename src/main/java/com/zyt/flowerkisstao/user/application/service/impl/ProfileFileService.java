package com.zyt.flowerkisstao.user.application.service.impl;

import com.zyt.flowerkisstao.shared.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.Iterator;
import java.util.Set;

/** 头像文件校验与落盘。文件名由服务端生成，避免路径穿越和同名覆盖。 */
@Service
@Slf4j
public class ProfileFileService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");
    private final Path avatarRoot;

    public ProfileFileService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.avatarRoot = Path.of(uploadDir).toAbsolutePath().normalize().resolve("avatars");
    }

    public String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择头像图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException("头像图片不能超过 2 MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new BizException("头像只支持 JPG、PNG 或 GIF 图片");
        }

        try (var input = file.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new BizException("无法识别头像图片，请选择有效的 JPG、PNG 或 GIF 文件");
            ImageReader reader = readers.next();
            BufferedImage image;
            try {
                reader.setInput(imageInput);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096) {
                    throw new BizException("头像尺寸无效，最大支持 4096 × 4096 像素");
                }
                image = reader.read(0);
            } finally {
                reader.dispose();
            }
            Files.createDirectories(avatarRoot);
            String filename = UUID.randomUUID() + ".png";
            Path target = avatarRoot.resolve(filename).normalize();
            double scale = Math.min(1.0, 512.0 / Math.max(image.getWidth(), image.getHeight()));
            BufferedImage avatar = new BufferedImage(Math.max(1, (int) (image.getWidth() * scale)),
                    Math.max(1, (int) (image.getHeight() * scale)), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = avatar.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(image, 0, 0, avatar.getWidth(), avatar.getHeight(), null);
            } finally {
                graphics.dispose();
            }
            // 重新编码，移除原始元数据及非图片内容，并限制最终头像尺寸。
            ImageIO.write(avatar, "png", target.toFile());
            return "/uploads/avatars/" + filename;
        } catch (IOException e) {
            throw new BizException("头像上传失败，请稍后重试");
        }
    }

    public void deleteAvatar(String url) {
        if (url == null || !url.matches("/uploads/avatars/[0-9a-f-]{36}\\.png")) return;
        try {
            Files.deleteIfExists(avatarRoot.resolve(url.substring(url.lastIndexOf('/') + 1)));
        } catch (IOException e) {
            log.warn("头像文件清理失败: {}", url);
        }
    }

}
