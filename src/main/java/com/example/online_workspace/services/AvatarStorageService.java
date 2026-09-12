package com.example.online_workspace.services;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import javax.imageio.ImageIO;

import com.example.online_workspace.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvatarStorageService {

	static final long MAX_SIZE_BYTES = 2L * 1024 * 1024;
	private static final int MAX_DIMENSION = 2048;

	private final Path storageDirectory;

	public AvatarStorageService(
		@Value("${app.avatar.storage-dir:/app/data/avatars}") String storageDirectory
	) {
		this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
	}

	public StoredAvatar save(long userId, MultipartFile file) {
		byte[] contents = readAndValidate(file);
		ImageFormat format = detectFormat(contents);
		try {
			Files.createDirectories(storageDirectory);
			Path temporary = Files.createTempFile(storageDirectory, userId + "-", ".uploading");
			try {
				Files.write(temporary, contents);
				Path target = pathFor(userId, format);
				move(temporary, target);
				for (ImageFormat other : ImageFormat.values()) {
					if (other != format) {
						Files.deleteIfExists(pathFor(userId, other));
					}
				}
				return new StoredAvatar(target, format.mediaType);
			} finally {
				Files.deleteIfExists(temporary);
			}
		} catch (IOException exception) {
			throw storageFailure();
		}
	}

	public Optional<StoredAvatar> find(long userId) {
		for (ImageFormat format : ImageFormat.values()) {
			Path path = pathFor(userId, format);
			if (Files.isRegularFile(path)) {
				return Optional.of(new StoredAvatar(path, format.mediaType));
			}
		}
		return Optional.empty();
	}

	private byte[] readAndValidate(MultipartFile file) {
		if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE_BYTES) {
			throw invalidImage();
		}
		try {
			return file.getBytes();
		} catch (IOException exception) {
			throw invalidImage();
		}
	}

	private ImageFormat detectFormat(byte[] contents) {
		for (ImageFormat format : ImageFormat.values()) {
			if (format.matches(contents) && isReadableImage(contents)) {
				return format;
			}
		}
		throw invalidImage();
	}

	private boolean isReadableImage(byte[] contents) {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(contents));
			return image != null
				&& image.getWidth() <= MAX_DIMENSION
				&& image.getHeight() <= MAX_DIMENSION;
		} catch (IOException exception) {
			return false;
		}
	}

	private Path pathFor(long userId, ImageFormat format) {
		return storageDirectory.resolve(userId + format.extension);
	}

	private void move(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.nio.file.AtomicMoveNotSupportedException exception) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private ApiException invalidImage() {
		return new ApiException(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"VALIDATION_FAILED",
			"画像は2MB以下のPNGまたはJPEGで指定してください。"
		);
	}

	private ResponseStatusException storageFailure() {
		return new ResponseStatusException(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"アイコンを保存できませんでした。"
		);
	}

	public record StoredAvatar(Path path, MediaType mediaType) {
	}

	private enum ImageFormat {
		PNG(".png", MediaType.IMAGE_PNG) {
			@Override
			boolean matches(byte[] contents) {
				byte[] signature = new byte[] {
					(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
				};
				return startsWith(contents, signature);
			}
		},
		JPEG(".jpg", MediaType.IMAGE_JPEG) {
			@Override
			boolean matches(byte[] contents) {
				return contents.length >= 3
					&& (contents[0] & 0xff) == 0xff
					&& (contents[1] & 0xff) == 0xd8
					&& (contents[2] & 0xff) == 0xff;
			}
		};

		private final String extension;
		private final MediaType mediaType;

		ImageFormat(String extension, MediaType mediaType) {
			this.extension = extension;
			this.mediaType = mediaType;
		}

		abstract boolean matches(byte[] contents);

		private static boolean startsWith(byte[] contents, byte[] prefix) {
			if (contents.length < prefix.length) {
				return false;
			}
			for (int index = 0; index < prefix.length; index++) {
				if (contents[index] != prefix[index]) {
					return false;
				}
			}
			return true;
		}
	}
}
