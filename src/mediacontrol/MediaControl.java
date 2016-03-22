package mediacontrol;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

public class MediaControl extends StackPane
{
    // Default time after which the controls are hidden if the mouse is not moved.
    private final int DEF_WAIT_TO_HIDE = 3;
    
    private MediaPlayer mp;
    private javafx.util.Duration mediaDuration;
    private boolean isFullScreen;
    private Timeline checkHideControlBar;
    private Instant lastMoved;
    private int waitHideControlBar;
    
    @FXML public MediaView mw;
    @FXML private HBox controlBar;
    @FXML private ImageView playImg;
    @FXML private ImageView fullScrImg;
    @FXML private ImageView resetImg;
    @FXML private Label displayTime;
    @FXML private Slider progressSlider;
    @FXML private ImageView volImg;
    @FXML private Slider volSlider;
    
    // Shorthand constructors.
    public MediaControl(MediaPlayer mediaPlayer)
    {
        this(mediaPlayer, 0,  null, 0);
    }    
    public MediaControl(MediaPlayer mediaPlayer, int waitHideControlBar)
    {
        this(mediaPlayer, waitHideControlBar,  null, 0);
    }    
    public MediaControl(MediaPlayer mediaPlayer, int waitHideControlBar, ReadOnlyDoubleProperty widthProp)
    {
        this(mediaPlayer, waitHideControlBar, widthProp, 0);
    }
    
    // Setup new instance of the MediaControl.
    public MediaControl(MediaPlayer mediaPlayer, int waitHideControlBar, ReadOnlyDoubleProperty widthProp, float maxWidth)
    {
        // MediaPlayer is mandatory.
        mp = mediaPlayer;
        
        mp.setOnReady(() -> {
             mediaDuration = mp.getCycleDuration();
            // Setup time label.
            displayTime.setText(secondsToTime((long) mp.getCurrentTime().toSeconds())
                    + "/"
                    + secondsToTime((long) mediaDuration.toSeconds()));
            // progressSlider moves while the video plays.
            mp.currentTimeProperty().addListener((Observable obs) -> {
                progressSlider.setValue(
                        mp.getCurrentTime().toSeconds() /
                                mediaDuration.toSeconds() * 100
                );
                displayTime.setText(secondsToTime((long) mp.getCurrentTime().toSeconds())
                        + "/"
                        + secondsToTime((long) mediaDuration.toSeconds()));
            });
        });
        
        // Default is not fullscreen.
        this.isFullScreen = false;
        
        // If user value is 0 or less use the default value.
        this.waitHideControlBar = waitHideControlBar > 0 ? waitHideControlBar : DEF_WAIT_TO_HIDE;
        
        // Set the last time the mouse moved to now.
        this.lastMoved = Instant.now();
        
        // Check every 0.5 seconds if 3 seconds have passed since the mouse last moved.
        checkHideControlBar = new Timeline(new KeyFrame(javafx.util.Duration.millis(500), ev -> {
            if (Duration.between(lastMoved, Instant.now()).getSeconds() > this.waitHideControlBar)
            {
                // Then hide the control bar.
                controlBar.setVisible(false);
            }
        }));
        checkHideControlBar.setCycleCount(Timeline.INDEFINITE);
        // Start checking.
        checkHideControlBar.play();
                 
        // Load the control from FXML.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MediaControl.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        // Setup the width of the control from the paramenters.
        mw.setMediaPlayer(mp);
        if (widthProp == null)
        {
            // Auto width.
        }
        else if (maxWidth == 0)
        {
            // Bind width of the control to the given width.
            mw.fitWidthProperty().bind(widthProp);
        }
        else
        {
            // Bind width of the control to the given width, but limiting it to the given limit.
            mw.fitWidthProperty().bind(
                Bindings.when(widthProp.greaterThan(new SimpleDoubleProperty(maxWidth)))
                    .then(maxWidth)
                    .otherwise(widthProp)
            );
        }
        // Bind the width of the control bar to the width of the MediaView.
        controlBar.maxWidthProperty().bind(mw.fitWidthProperty());
        controlBar.setMaxHeight(30);
        
        // Setup progressSlider.
        progressSlider.setDisable(true);
        // Setup buttons.
        playImg.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> handlePlay(e));
        fullScrImg.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> handleFullScr(e));
        resetImg.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> handleReset(e));
        fullScrImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/FullScr.png").toString()));
        volImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Vol.png").toString()));
        resetImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Reset.png").toString()));
        volSlider.setValue(mp.getVolume());
        volSlider.valueProperty().bindBidirectional(mp.volumeProperty());
        
        // When the video ends if it's not still looping take it to the beginning and stop it.
        mp.setOnEndOfMedia(() -> {
            if (mp.getCurrentCount() == mp.getCycleCount())
            {
                mp.seek(javafx.util.Duration.ZERO);
                mp.pause();
                playImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Play.png").toString()));
                refreshAll();
            }
        });
        
        if (mp.isAutoPlay() && ! this.isFullScreen)
        {
            playImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Pause.png").toString()));
        }
        
        refreshAll();
    }
    
    private void refreshAll()
    {
        Status status = mp.getStatus();
        if (status == Status.UNKNOWN
                || status == Status.HALTED
                || status == Status.PAUSED
                || status == Status.READY
                || status == Status.STOPPED)
        {
            playImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Play.png").toString()));
        }
        else
        {
            playImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Pause.png").toString()));
        }
    }
    
    
    // When the mouse is moved the control bar is displayed and the countodwn
    // to hide it after waitHideControlBar seconds starts.
    @FXML private void handleMouseMoved()
    {
        controlBar.setVisible(true);
        lastMoved = Instant.now();
    }
    
    // Setup button actions.
    
    // Play/pause button.
    @FXML private void handlePlay(MouseEvent e)
    {
        e.consume();
        Status status = mp.getStatus();
        if (status == Status.UNKNOWN || status == Status.HALTED)
        {
            return;
        }
        if (status == Status.PAUSED
                || status == Status.READY
                || status == Status.STOPPED)
        {
            mp.play();
            playImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Pause.png").toString()));
        }
        else
        {
            mp.pause();
            playImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Play.png").toString()));
        }
    }
        
    // Fullscreen button
    @FXML private void handleFullScr(MouseEvent e)
    {
        e.consume();
        if (isFullScreen)
        {
            // Close if already active.
            ((Stage) mw.getScene().getWindow()).close();
        }
        else
        {
            // Start if not already active.
            Stage stage = new Stage();
            MediaControl nmc = new MediaControl(mp, waitHideControlBar, stage.widthProperty());
            nmc.isFullScreen = true;
            Scene scene = new Scene(nmc);
            // When escape is pressed the window closes.
            scene.setOnKeyPressed(ke -> {
                if (ke.getCode() == KeyCode.ESCAPE) {
                    stage.close();
                }
            });
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.showAndWait();
            refreshAll();
        }
    }
    
    // Reset button
    @FXML private void handleReset(MouseEvent e)
    {
        e.consume();
        mp.seek(javafx.util.Duration.ZERO);
        mp.pause();
        playImg.setImage(new Image(getClass().getResource("/mediacontrol/buttonimages/Play.png").toString()));
    }
    
    private String secondsToTime(long secs)
    {
        if (secs >= 3600)
        {
            return String.format("%02d:%02d:%02d", secs/3600, (secs/60)%60, secs%60);
        }
        else
        {
            return String.format("%02d:%02d", (secs/60)%60, secs%60);
        }
    }
}
