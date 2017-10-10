package net.gorry.android.input.nicownng;

import android.os.AsyncTask;
import android.util.Log;

public class TextCandidateTask extends AsyncTask<Integer, Integer, Integer> {
	private static final int kFIRST_WAIT  = 500;
	private static final int kSECOND_WAIT = 50;
	private static final int kUPDATE_WAIT = 50;

	private TextCandidatesViewManager mManager = null;
	private boolean mIsCancel = false;
	private boolean mIsUpdate = false;
	private int     mMaxLine  = 0;
	private final boolean[] mIsEnd;
	/*
	 * 
	 */
	public TextCandidateTask(final TextCandidatesViewManager inmanager) {
		mManager   = inmanager;
		mIsCancel  = false;
		mIsUpdate  = false;
		mMaxLine   = mManager.getMaxLine();
		mIsEnd = new boolean[mManager.getMaxLine()];
	}
	/*
	 * 
	 */
	@Override
	protected Integer doInBackground(final Integer... params) {
		if (null == mManager.mConverter) {
			return 0;
		}
		int retcode = 0;
		/* Get candidates */
		final int maxline = mManager.getMaxLine();
		int iI = 0;
		int endcount;
		for (iI = 0; iI < maxline; ++iI) {
			mIsEnd[iI] = false;
		}

		retcode = waitTask(kFIRST_WAIT);
		if (1 == retcode) {
			return 1;
		}

		while (true) {
			if (true == mIsUpdate) {
				retcode = 0;//waitTask(kSECOND_WAIT);
				if (1 == retcode) {
					return 1;
				}
			}
			// check endcount
			endcount = 0;
			for (iI = 0; iI < maxline; ++iI) {
				if (true == mIsEnd[iI]) {
					endcount++;
				}
			}
			if (endcount >= maxline) {
				//endTask();
				return 0;
			}
			if (true == mIsCancel) {
				return 1;
			}
			mIsUpdate = true;

			publishProgress(0);
		} // while
	}
	/*
	 * 
	 */
	private int waitTask(final int waittime) {
		int count = 0;
		while(count < waittime) {
			if (true == mIsCancel) {
				return 1;
			}

//			/*
			try {
				Thread.sleep(kUPDATE_WAIT);
			} catch (final Exception e) {
				return 1;
			}
//			*/

			//SystemClock.sleep(kUPDATE_WAIT);
			count += kUPDATE_WAIT;
		}
		return 0;
	}
	/*
	 * 
	 */
	private void endTask() {
		int retcode = 0;
		while(true) {
			retcode = waitTask(kSECOND_WAIT);
			if (1 == retcode) {
				return;
			}
		}
	}
	/*
	 * 
	 */
	@Override
	protected void onPostExecute(final Integer result) {
		if ((null != result) && (0 == result)) {
			mManager.display1stLastSetup();
		}
	}
	/*
	 * 
	 */
	@Override
	protected void onCancelled() {
		mIsCancel = true;
		mManager.cancelTask();
	}
	/*
	 * 
	 */
	@Override
	protected void onProgressUpdate(final Integer...params) {
		if (true == mIsCancel) {
			mIsUpdate = false;
			return;
		}
		boolean inv = false;
		final int maxline = mMaxLine;
		int totalwidth = 0;
		for (int j=0; j<TextCandidatesViewManager.FULL_VIEW_DIV; j++) {
			int w = 0;
			for (int i = 0; i < maxline; ++i) {
				final int getwidth = mManager.calc1stCandidates(i, mManager.mCandidateViewWidth);
				if (getwidth >= w) {
					w = getwidth;
				}
			}
			for (int i = 0; i < maxline; ++i) {
				if (w == 0) {
					mIsEnd[i] = true;	// end draw line
				}
				else{
					mIsEnd[i] = mManager.display1stCandidates(i, w);
					inv = true;
				}
			}
			totalwidth += w;
			if (totalwidth >= mManager.mCandidateViewWidth) break;
		}
		if (inv) {
			mManager.invalidate1stView();
		}
		mIsUpdate = false;
	}
}
