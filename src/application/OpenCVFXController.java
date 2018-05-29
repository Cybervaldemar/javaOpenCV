package application;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import application.Utils.Utils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;


public class OpenCVFXController
{
	@FXML
	private Button button;
	@FXML
	private ImageView currentFrame;
	@FXML
	private CheckBox grayScaleCheckBox;
	@FXML
	private CheckBox gaussBlurCheckBox;
	@FXML
	private CheckBox cannyCheckBox;
	@FXML
	private Slider thresholdSlider;
	@FXML
	private Label captureLabel;
	
	private ScheduledExecutorService timer;
	private VideoCapture capture = new VideoCapture();
	final FileChooser fileChooser = new FileChooser();
	private boolean cameraActive = false;
	private static int cameraId = 0;
	
	private String captureFile = "";
	

	@FXML
	protected void startCamera(ActionEvent event)
	{
		if (!this.cameraActive)
		{
			// start the video capture
			
			if (captureFile.length() > 0) {
				this.capture.open(captureFile);
			} else {
				this.capture.open(0);
			}
			//this.capture.open()
			
			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						Mat frame = grabFrame();
						
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(currentFrame, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				this.button.setText("Stop Camera");
			}
			else
			{
				System.err.println("Impossible to open the connection...");
			}
		}
		else
		{
			this.cameraActive = false;
			this.button.setText("Start Camera");
			
			this.stopAcquisition();
		}
	}

	private Mat grabFrame()
	{
		Mat frame = new Mat();
		
		if (this.capture.isOpened())
		{
			try
			{
				this.capture.read(frame);
				
				if (!frame.empty())
				{
					if (grayScaleCheckBox.isSelected()) {
						Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
					}
					if (gaussBlurCheckBox.isSelected()) {
						Imgproc.GaussianBlur(frame, frame, new Size(15, 15), 0);
					}
					if (this.cannyCheckBox.isSelected()){
					    frame = this.doCanny(frame);
					}
				}
				
			}
			catch (Exception e)
			{
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return frame;
	}
	

	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			this.capture.release();
		}
	}
	
	
	private Mat doCanny(Mat frame)
	{
		// init
		Mat grayImage = new Mat();
		Mat detectedEdges = new Mat();
		
		Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);
		
		Imgproc.blur(grayImage, detectedEdges, new Size(3, 3));
		
		Imgproc.Canny(detectedEdges, detectedEdges, this.thresholdSlider.getValue(), this.thresholdSlider.getValue() * 3);
		
		// using Canny's output as a mask, display the result
		Mat dest = new Mat();
		frame.copyTo(dest, detectedEdges);
		
		return dest;
	}
	
	@FXML
	protected void getFileForCapturing(ActionEvent event)
	{
		File newVideo = fileChooser.showOpenDialog(null);
		this.captureFile = newVideo.toString();
		this.captureLabel.setText("Capturing the " + newVideo.getName() + " movie");
	}
	
	@FXML
	protected void setCapturingFromCamera(ActionEvent event)
	{
		this.captureFile = "";
		this.captureLabel.setText("Capturing the camera");
	}
	

	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}
	
	protected void setClosed()
	{
		this.stopAcquisition();
	}
	
}