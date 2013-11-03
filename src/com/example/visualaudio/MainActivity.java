package com.example.visualaudio;

import ca.uol.aig.fftpack.RealDoubleFFT;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {

	int frequency = 8000;
	int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	RealDoubleFFT fftTrans;
	int blockSize = 256;
	Button startStopBtn;
	boolean started = false;
	RecordAudioTask recordAudioTask;
	ImageView imgView;
	Bitmap bitmap;
	Canvas canvas;
	Paint paint;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		startStopBtn = (Button) findViewById(R.id.startStopBtn);
		startStopBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (started) {
					started = false;
					startStopBtn.setText("Start");
					recordAudioTask.cancel(true);
				} else {
					started = true;
					recordAudioTask = new RecordAudioTask();
					startStopBtn.setText("Stop");
					recordAudioTask.execute();
				}
			}
		});

		fftTrans = new RealDoubleFFT(blockSize);

		imgView = (ImageView) findViewById(R.id.imgView);
		bitmap = Bitmap.createBitmap(256, 100, Bitmap.Config.ARGB_8888);

		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setColor(Color.GREEN);
		imgView.setImageBitmap(bitmap);
	}

	private class RecordAudioTask extends AsyncTask<Void, double[], Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				int bufferSize = AudioRecord.getMinBufferSize(frequency,
						channelConfig, audioFormat);
				Log.v("bufSize", String.valueOf(bufferSize));
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, frequency,
						channelConfig, audioFormat, bufferSize);

				short[] audioBuffer = new short[blockSize];
				double[] toTrans = new double[blockSize];

				audioRecord.startRecording();

				while (started) {
					int result = audioRecord.read(audioBuffer, 0, blockSize);

					for (int i = 0; i < blockSize && i < result; i++) {
						toTrans[i] = (double) audioBuffer[i] / Short.MAX_VALUE;
					}
					fftTrans.ft(toTrans);
					publishProgress(toTrans);
				}
				audioRecord.stop();
			} catch (Throwable t) {
				Log.e("AudioRecord", "Recording failed");
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(double[]... values) {

			canvas.drawColor(Color.BLACK);
			for (int i = 0; i < values[0].length; i++) {
				int x = i;
				int downy = (int) (100 - (values[0][i] * 10));
				int upy = 100;

				canvas.drawLine(x, downy, x, upy, paint);
			}
			imgView.invalidate();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
