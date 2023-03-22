package com.grpc.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.google.protobuf.ByteString;
import com.grpc.fileupload.FileServiceGrpc;
import com.grpc.fileupload.FileUploadRequest;
import com.grpc.fileupload.FileUploadResponse;
import com.grpc.fileupload.Status;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class FileUploadService extends FileServiceGrpc.FileServiceImplBase {

	private static final Path SERVER_BASE_PATH = Paths.get("/etc");

	@Override
	public StreamObserver<FileUploadRequest> upload(StreamObserver<FileUploadResponse> responseObserver) {
		return new StreamObserver<FileUploadRequest>() {
			// upload context variables
			OutputStream writer;
			Status status = Status.IN_PROGRESS;

			@Override
			public void onNext(FileUploadRequest fileUploadRequest) {
				System.out.println("onNext...");
				try {
					writer = getFilePath(fileUploadRequest);
					writeFile(writer, fileUploadRequest.getFile().getContent());
				} catch (IOException e) {
					this.onError(e);
				}
			}

			@Override
			public void onError(Throwable throwable) {
				status = Status.FAILED;
				this.onCompleted();
			}

			@Override
			public void onCompleted() {
				System.out.println("onCompleted.......");
				closeFile(writer);
				System.out.println("status......." + status);
				status = Status.IN_PROGRESS.equals(status) ? Status.SUCCESS : status;
				System.out.println("after status......." + status);
				FileUploadResponse response = FileUploadResponse.newBuilder().setStatus(status).build();
				responseObserver.onNext(response);
				responseObserver.onCompleted();
			}
		};
	}

	private OutputStream getFilePath(FileUploadRequest request) throws IOException {
		String fileName = request.getFile().getName() + "." + request.getFile().getType();
		return Files.newOutputStream(SERVER_BASE_PATH.resolve(fileName), StandardOpenOption.CREATE,
				StandardOpenOption.APPEND);
	}

	private void writeFile(OutputStream writer, ByteString content) throws IOException {
		writer.write(content.toByteArray());
		writer.flush();
	}

	private void closeFile(OutputStream writer) {
		try {
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}