package com.zyt.flowerkisstao.user;

import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.user.application.service.impl.ProfileFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class ProfileFileServiceTest {
    @TempDir Path root;

    @Test
    void storesResizedImageWithServerGeneratedNameAndCanRemoveIt() throws Exception {
        BufferedImage input = new BufferedImage(1024, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(input, "png", bytes);
        ProfileFileService service = new ProfileFileService(root.toString());
        String url = service.storeAvatar(new MockMultipartFile("file", "../../outside.png", "image/png", bytes.toByteArray()));
        assertThat(url).matches("/uploads/avatars/[0-9a-f-]{36}\\.png");
        Path file = root.resolve(url.substring("/uploads/".length()));
        BufferedImage stored = ImageIO.read(file.toFile());
        assertThat(stored.getWidth()).isEqualTo(512);
        assertThat(stored.getHeight()).isEqualTo(300);
        service.deleteAvatar(url);
        assertThat(file).doesNotExist();
    }

    @Test
    void rejectsDisguisedNonImageAndOversizedFiles() {
        ProfileFileService service = new ProfileFileService(root.toString());
        assertThatThrownBy(() -> service.storeAvatar(new MockMultipartFile("file", "fake.png", "image/png", "<html/>".getBytes())))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.storeAvatar(new MockMultipartFile("file", "large.png", "image/png", new byte[2 * 1024 * 1024 + 1])))
                .isInstanceOf(BizException.class).hasMessageContaining("2 MB");
    }

    @Test
    void cleanupCannotDeleteOutsideAvatarDirectory() throws Exception {
        Path sentinel = Files.writeString(root.resolve("keep.txt"), "keep");
        new ProfileFileService(root.toString()).deleteAvatar("/uploads/avatars/../../keep.txt");
        assertThat(sentinel).exists();
    }
}
