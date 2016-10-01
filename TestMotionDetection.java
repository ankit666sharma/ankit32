
/**
 * Copyright:    Copyright (c) 2002 by Konrad Rzeszutek
 *
 *
 *
 *
 *    This file is part of Motion Detection toolkit.
 *
 *    Motion Detection toolkit is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    Motion Detection toolkit is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Foobar; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.Format;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.datasink.*;
import javax.media.control.*;
/**
 * Sample program to test the MotionDetectionEffect.
 */
public class TestMotionDetection extends Frame implements ControllerListener {

    Processor p;
    DataSink fileW = null;
    Object waitSync = new Object();
    boolean stateTransitionOK = true;

    public TestMotionDetection() {
      super("Test Motion Detection");
    }
    /**
     * Given a datasource, create a processor and use that processor
     * as a player to playback the media.
     *
     * During the processor's Configured state, the MotionDetectionEffect
     * and TimeStampEffect is
     * inserted into the video track.
     *
     */
    public boolean open(MediaLocator ds) {

	try {
	    p = Manager.createProcessor(ds);
	} catch (Exception e) {
	    System.err.println("Failed to create a processor from the given datasource: " + e);
	    return false;
	}

	p.addControllerListener(this);


	// Put the Processor into configured state.
	p.configure();
	if (!waitForState(p.Configured)) {
	    System.err.println("Failed to configure the processor.");
	    return false;
	}

	// So I can use it as a player.

	p.setContentDescriptor(null);
	// Obtain the track controls.
	TrackControl tc[] = p.getTrackControls();

	if (tc == null) {
	    System.err.println("Failed to obtain track controls from the processor.");
	    return false;
	}

	// Search for the track control for the video track.
	TrackControl videoTrack = null;

	for (int i = 0; i < tc.length; i++) {
	    if (tc[i].getFormat() instanceof VideoFormat) {
		videoTrack = tc[i];
		break;
	    }
	}

	if (videoTrack == null) {
	    System.err.println("The input media does not contain a video track.");
	    return false;
	}


	// Instantiate and set the frame access codec to the data flow path.

	try {
	    Codec codec[] = {  new MotionDetectionEffect(), new TimeStampEffect()};
	    videoTrack.setCodecChain(codec);
	} catch (UnsupportedPlugInException e) {
	    System.err.println("The processor does not support effects.");
	}

	// Realize the processor.
	
	p.prefetch();
	if (!waitForState(p.Prefetched)) {
	    System.err.println("Failed to realize the processor.");
	    return false;
	}
	// Display the visual & control component if there's one.

        // Get the player. Or construct the player from the processor

	setLayout(new BorderLayout());

	Component cc;

	Component vc;
	if ((vc = p.getVisualComponent()) != null) {
	    add("Center", vc);
	}

	if ((cc = p.getControlPanelComponent()) != null) {
	    add("South", cc);
	}

	// Start the processor.
	p.start();

	setVisible(true);

	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent we) {
		p.close();
		System.exit(0);
	    }
	});
      
        p.start();

	return true;
    }

    public void addNotify() {
	super.addNotify();
	pack();
    }

    /**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     */
    boolean waitForState(int state) {
	synchronized (waitSync) {
	    try {
		while (p.getState() != state && stateTransitionOK)
		    waitSync.wait();
	    } catch (Exception e) {}
	}
	return stateTransitionOK;
    }


    /**
     * Controller Listener.
     */
    public void controllerUpdate(ControllerEvent evt) {

        System.out.println(this.getClass().getName()+evt);
	if (evt instanceof ConfigureCompleteEvent ||
	    evt instanceof RealizeCompleteEvent ||
	    evt instanceof PrefetchCompleteEvent) {
	    synchronized (waitSync) {
		stateTransitionOK = true;
		waitSync.notifyAll();
	    }
	} else if (evt instanceof ResourceUnavailableEvent) {
	    synchronized (waitSync) {
		stateTransitionOK = false;
		waitSync.notifyAll();
	    }
	} else if (evt instanceof EndOfMediaEvent) {
	    p.close();
	    System.exit(0);
	}
    }

  public static void main(String [] args) {

        if (args.length == 0) {
            prUsage();
            System.exit(0);
        }

        String url = args[0];

        if (url.indexOf(":") < 0) {
            prUsage();
            System.exit(0);
        }

        MediaLocator ml;
   if ((ml = new MediaLocator(url)) == null) {
            System.err.println("Cannot build media locator from: " + url);
            System.exit(0);
        }

        TestMotionDetection fa = new TestMotionDetection();

        if (!fa.open(ml))
            System.exit(0);
    }

    static void prUsage() {
        System.err.println("Usage: java TestMotionDetection <url>");
    }


}
