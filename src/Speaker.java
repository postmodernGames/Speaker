

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.*;


public class Speaker
{
    private static final int	EXTERNAL_BUFFER_SIZE = 128000;



    public static void main(String[] args)
    {


        String inFilename = args[0];
        String outFilename = args[1];
        
        File	soundFile = new File(inFilename);
        File captureFile = new File(outFilename);

        AudioInputStream audioInputStream = null;
        AudioInputStream audioMicStream = null;
        try
        {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            audioMicStream = AudioSystem.getAudioInputStream(captureFile);
        }
        catch (Exception e)
        {
			/*
			  In case of an exception, we dump the exception
			  including the stack trace to the console output.
			  Then, we exit the program.
			*/
            e.printStackTrace();
            System.exit(1);
        }


        AudioFormat	audioFormat = audioInputStream.getFormat();
        AudioFormat targetFormat = audioMicStream.getFormat();

		/*
		  Asking for a line is a rather tricky thing.
		  We have to construct an Info object that specifies
		  the desired properties for the line.
		  First, we have to say which kind of line we want. The
		  possibilities are: SourceDataLine (for playback), Clip
		  (for repeated playback)	and TargetDataLine (for
		  recording).
		  Here, we want to do normal playback, so we ask for
		  a SourceDataLine.
		  Then, we have to pass an AudioFormat object, so that
		  the Line knows which format the data passed to it
		  will have.
		  Furthermore, we can give Java Sound a hint about how
		  big the internal buffer for the line should be. This
		  isn't used here, signaling that we
		  don't care about the exact size. Java Sound will use
		  some default value for the buffer size.
		*/

        TargetDataLine micLine = null;
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class,
                targetFormat);

        SourceDataLine	line = null;
        DataLine.Info	info = new DataLine.Info(SourceDataLine.class,
                audioFormat);
        try
        {
            line = (SourceDataLine) AudioSystem.getLine(info);
            micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
			/*
			  The line is there, but it is not yet ready to
			  receive audio data. We have to open the line.
			*/
            line.open(audioFormat);
            micLine.open(targetFormat);
        }
        catch (LineUnavailableException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

		/*
		  Still not enough. The line now can receive data,
		  but will not pass them on to the audio output device
		  (which means to your sound card). This has to be
		  activated.
		*/
        //line.start();

        // Assume that the TargetDataLine, line, has already
// been obtained and opened.
        ByteArrayOutputStream out  = new ByteArrayOutputStream();
        int numBytesRead;
        byte[] data = new byte[line.getBufferSize() / 5];

// Begin audio capture.
        micLine.start();

// Here, stopped is a global boolean set by another thread.
        boolean stopped = false;

        while (!stopped) {
            // Read the next chunk of data from the TargetDataLine.
            numBytesRead =  micLine.read(data, 0, data.length);
            // Save this chunk of data.
            out.write(data, 0, numBytesRead);
        }

		/*
		  Ok, finally the line is prepared. Now comes the real
		  job: we have to write data to the line. We do this
		  in a loop. First, we read data from the
		  AudioInputStream to a buffer. Then, we write from
		  this buffer to the Line. This is done until the end
		  of the file is reached, which is detected by a
		  return value of -1 from the read method of the
		  AudioInputStream.
		*/
		/*
        int	nBytesRead = 0;
        byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];
        while (nBytesRead != -1)
        {
            try
            {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            if (nBytesRead >= 0)
            {
                int	nBytesWritten = line.write(abData, 0, nBytesRead);
            }
        }

		/*
		  Wait until all data are played.
		  This is only necessary because of the bug noted below.
		  (If we do not wait, we would interrupt the playback by
		  prematurely closing the line and exiting the VM.)

		  Thanks to Margie Fitch for bringing me on the right
		  path to this solution.
		*/
        micLine.drain();

		/*
		  All data are played. We can close the shop.
		*/
        micLine.close();

		/*
		  There is a bug in the jdk1.3/1.4.
		  It prevents correct termination of the VM.
		  So we have to exit ourselves.
		*/

      //  System.exit(0);
    }
}


