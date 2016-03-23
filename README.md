# MediaControl

Media control is an extremely simple custom control for JavaFX composed of a MediaView and some controls in a bar.

NB: The version v2.0 is extremely better than the v1.0: bugs have been fixed, code was polished and general usability is improved.

## Functionalities

With this control it is possible to play a video/audio file inside the JavaFX application, pause and resume it, control its volume and watch it fullscreen.

There are lots of constructor which let you setup MediaControl exactly as you need it. You have control over:

 - media file;

 - autoplay;

 - looping (infinte or finite amount of repetitions);

 - time after which the control bar and the cursor are hidden;

 - width of the control (binding);

 - limit of the width (upper limit of the binding);

 - moment on which to start the media;

 - volume of the media.

## Limitations

The progressbar is not interactive: it shows the current time position of the media and the total time, but it isn't possible to skip to a certain point of the media via the progress bar.

In some cases when alternating between fullscreen and normal view the loop count can get screwed up, generally resulting in the media playing some loops more than intended.

## License

MediaControl is licensed under MIT license (se the [LICENSE file](https://github.com/GioBonvi/MediaControl/blob/master/LICENSE)).