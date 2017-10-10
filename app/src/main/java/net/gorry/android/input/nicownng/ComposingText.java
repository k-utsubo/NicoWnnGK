/*
 * Copyright (C) 2008,2009  OMRON SOFTWARE Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.gorry.android.input.nicownng;

import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;

/**
 * The container class of composing string.
 *
 * This interface is for the class includes information about the
 * input string, the converted string and its decoration.
 * {@link LetterConverter} and {@link WnnEngine} get the input string from it, and
 * store the converted string into it.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class ComposingText {
	/**
	 * Text layer 0.
	 * <br>
	 * This text layer holds key strokes.<br>
	 * (ex) Romaji in Japanese.  Parts of Hangul in Korean.
	 */
	public static final int LAYER0  = 0;
	/**
	 * Text layer 1.
	 * <br>
	 * This text layer holds the result of the letter converter.<br>
	 * (ex) Hiragana in Japanese. Pinyin in Chinese. Hangul in Korean.
	 */
	public static final int LAYER1  = 1;
	/**
	 * Text layer 2.
	 * <br>
	 * This text layer holds the result of the consecutive clause converter.<br>
	 * (ex) the result of Kana-to-Kanji conversion in Japanese,
	 *      Pinyin-to-Kanji conversion in Chinese, Hangul-to-Hanja conversion in Korean language.
	 */
	public static final int LAYER2  = 2;
	/** Maximum number of layers */
	public static final int MAX_LAYER = 3;

	/** Composing text's layer data */
	protected ArrayList<StrSegment>[] mStringLayer;
	/** Cursor position */
	protected int[] mCursor;

	/**
	 * Constructor
	 */
	public ComposingText() {
		mStringLayer = new ArrayList[MAX_LAYER];
		mCursor = new int[MAX_LAYER];
		for (int i = 0; i < MAX_LAYER; i++) {
			mStringLayer[i] = new ArrayList<StrSegment>();
			mCursor[i] = 0;
		}
	}

	/**
	 * Output internal information to the log.
	 */
	public void debugout() {
		for (int i = 0; i < MAX_LAYER; i++) {
			Log.d("NicoWnnG", "ComposingText["+i+"]");
			Log.d("NicoWnnG", "  cur = " + mCursor[i]);
			String tmp = "";
			for (final Iterator<StrSegment> it = mStringLayer[i].iterator(); it.hasNext();) {
				final StrSegment ss = it.next();
				tmp += "(" + ss.string + "," + ss.from + "," + ss.to + ")";
			}
			Log.d("NicoWnnG", "  str = "+tmp);
		}
	}

	/**
	 * Get a {@link StrSegment} at the position specified.
	 *
	 * @param layer     Layer
	 * @param pos       Position (<0 : the tail segment)
	 *
	 * @return          The segment; {@code null} if error occurs.
	 */
	public StrSegment getStrSegment(final int layer, int pos) {
		try {
			final ArrayList<StrSegment> strLayer = mStringLayer[layer];
			if (pos < 0) {
				pos = strLayer.size() - 1;
			}
			if ((pos >= strLayer.size()) || (pos < 0)) {
				return null;
			}
			return strLayer.get(pos);
		} catch (final Exception ex) {
			return null;
		}
	}

	/**
	 * Convert the range of segments to a string.
	 *
	 * @param layer     Layer
	 * @param from      Convert range from
	 * @param to        Convert range to
	 * @return          The string converted; {@code null} if error occurs.
	 */
	public String toString(final int layer, final int from, final int to) {
		try {
			final StringBuffer buf = new StringBuffer();
			final ArrayList<StrSegment> strLayer = mStringLayer[layer];

			for (int i = from; i <= to; i++) {
				if (i < strLayer.size()) {
					final StrSegment ss = strLayer.get(i);
					buf.append(ss.string);
				}
			}
			return buf.toString();
		} catch (final Exception ex) {
			return null;
		}
	}

	/**
	 * Convert segments of the layer to a string.
	 *
	 * @param layer     Layer
	 * @return          The string converted; {@code null} if error occurs.
	 */
	public String toString(final int layer) {
		return this.toString(layer, 0, mStringLayer[layer].size() - 1);
	}

	/**
	 * Update the upper layer's data.
	 *
	 * @param layer         The base layer
	 * @param mod_from      Modified from
	 * @param mod_len       Length after modified (# of StrSegments from {@code mod_from})
	 * @param org_len       Length before modified (# of StrSegments from {@code mod_from})
	 */
	private void modifyUpper(final int layer, final int mod_from, final int mod_len, final int org_len) {
		if (layer >= MAX_LAYER - 1) {
			/* no layer above */
			return;
		}

		final int uplayer = layer + 1;
		final ArrayList<StrSegment> strUplayer = mStringLayer[uplayer];
		if (strUplayer.size() <= 0) {
			/*
			 * if there is no element on above layer,
			 * add a element includes whole elements of the lower layer.
			 */
			strUplayer.add(new StrSegment(toString(layer), 0, mStringLayer[layer].size() - 1));
			modifyUpper(uplayer, 0, 1, 0);
			return;
		}

		final int mod_to = mod_from + ((mod_len == 0)? 0 : (mod_len - 1));
		final int org_to = mod_from + ((org_len == 0)? 0 : (org_len - 1));
		final StrSegment last = strUplayer.get(strUplayer.size() - 1);
		if (last.to < mod_from) {
			/* add at the tail */
			last.to = mod_to;
			last.string = toString(layer, last.from, last.to);
			modifyUpper(uplayer, strUplayer.size()-1, 1, 1);
			return;
		}

		int uplayer_mod_from = -1;
		int uplayer_org_to = -1;
		for (int i = 0; i < strUplayer.size(); i++) {
			final StrSegment ss = strUplayer.get(i);
			if (ss.from > mod_from) {
				if (ss.to <= org_to) {
					/* the segment is included */
					if (uplayer_mod_from < 0) {
						uplayer_mod_from = i;
					}
					uplayer_org_to = i;
				} else {
					/* included in this segment */
					uplayer_org_to = i;
					break;
				}
			} else {
				if ((org_len == 0) && (ss.from == mod_from)) {
					/* when an element is added */
					uplayer_mod_from = i - 1;
					uplayer_org_to   = i - 1;
					break;
				} else {
					/* start from this segment */
					uplayer_mod_from = i;
					uplayer_org_to = i;
					if (ss.to >= org_to) {
						break;
					}
				}
			}
		}

		final int diff = mod_len - org_len;
		if (uplayer_mod_from >= 0) {
			/* update an element */
			StrSegment ss = strUplayer.get(uplayer_mod_from);
			int last_to = ss.to;
			final int next = uplayer_mod_from + 1;
			for (int i = next; i <= uplayer_org_to; i++) {
				ss = strUplayer.get(next);
				if (last_to > ss.to) {
					last_to = ss.to;
				}
				strUplayer.remove(next);
			}
			ss.to = (last_to < mod_to)? mod_to : (last_to + diff);

			ss.string = toString(layer, ss.from, ss.to);

			for (int i = next; i < strUplayer.size(); i++) {
				ss = strUplayer.get(i);
				ss.from += diff;
				ss.to   += diff;
			}

			modifyUpper(uplayer, uplayer_mod_from, 1, uplayer_org_to - uplayer_mod_from + 1);
		} else {
			/* add an element at the head */
			StrSegment ss = new StrSegment(toString(layer, mod_from, mod_to),
					mod_from, mod_to);
			strUplayer.add(0, ss);
			for (int i = 1; i < strUplayer.size(); i++) {
				ss = strUplayer.get(i);
				ss.from += diff;
				ss.to   += diff;
			}
			modifyUpper(uplayer, 0, 1, 0);
		}

		return;
	}

	/**
	 * Insert a {@link StrSegment} at the cursor position.
	 * 
	 * @param layer Layer to insert
	 * @param str   String
	 **/
	public void insertStrSegment(final int layer, final StrSegment str) {
		final int cursor = mCursor[layer];
		mStringLayer[layer].add(cursor, str);
		modifyUpper(layer, cursor, 1, 0);
		setCursor(layer, cursor + 1);
	}

	/**
	 * Insert a {@link StrSegment} at the cursor position(without merging to the previous segment).
	 * <p>
	 * @param layer1        Layer to insert
	 * @param layer2        Never merge to the previous segment from {@code layer1} to {@code layer2}.
	 * @param str           String
	 **/
	public void insertStrSegment(final int layer1, final int layer2, final StrSegment str) {
		mStringLayer[layer1].add(mCursor[layer1], str);
		mCursor[layer1]++;

		for (int i = layer1 + 1; i <= layer2; i++) {
			final int pos = mCursor[i-1] - 1;
			final StrSegment tmp = new StrSegment(str.string, pos, pos);
			final ArrayList<StrSegment> strLayer = mStringLayer[i];
			strLayer.add(mCursor[i], tmp);
			mCursor[i]++;
			for (int j = mCursor[i]; j < strLayer.size(); j++) {
				final StrSegment ss = strLayer.get(j);
				ss.from++;
				ss.to++;
			}
		}
		final int cursor = mCursor[layer2];
		modifyUpper(layer2, cursor - 1, 1, 0);
		setCursor(layer2, cursor);
	}

	/**
	 * Replace segments at the range specified.
	 *
	 * @param layer     Layer
	 * @param str       String segment array to replace
	 * @param from      Replace from
	 * @param to        Replace to
	 **/
	protected void replaceStrSegment0(final int layer, final StrSegment[] str, int from, int to) {
		final ArrayList<StrSegment> strLayer = mStringLayer[layer];

		if ((from < 0) || (from > strLayer.size())) {
			// from = strLayer.size();
			from = 0;
		}
		if ((to < 0) || (to > strLayer.size())) {
			to = strLayer.size();
		}
		for (int i = from; i <= to; i++) {
			if (from < strLayer.size()) {
				strLayer.remove(from);
			}
		}
		for (int i = str.length - 1; i >= 0; i--) {
			strLayer.add(from, str[i]);
		}

		modifyUpper(layer, from, str.length, to - from + 1);
	}

	/**
	 * Replace segments at the range specified.
	 *
	 * @param layer     Layer
	 * @param str       String segment array to replace
	 * @param num       Size of string segment array
	 **/
	public void replaceStrSegment(final int layer, final StrSegment[] str, final int num) {
		final int cursor = mCursor[layer];
		final int cursor1 = (cursor-num < 0) ? 0 : cursor-num;
		final int cursor2 = (cursor-1 < 0) ? 0 : cursor-1;
		replaceStrSegment0(layer, str, cursor1, cursor2);
		setCursor(layer, cursor + str.length - num);
	}

	/**
	 * Replace the segment at the cursor.
	 *
	 * @param layer     Layer
	 * @param str       String segment to replace
	 **/
	public void replaceStrSegment(final int layer, final StrSegment[] str) {
		final int cursor = mCursor[layer];
		final int cursor2 = (cursor-1 < 0) ? 0 : cursor-1;
		replaceStrSegment0(layer, str, cursor2, cursor2);
		setCursor(layer, cursor + str.length - 1);
	}

	/**
	 * Set the segment.
	 *
	 * @param layer     Layer
	 * @param str       String segment to replace
	 **/
	public void setStrSegment(final int layer, final StrSegment[] str) {
		replaceStrSegment0(layer, str, 0, mStringLayer[layer].size()-1);
	}

	/**
	 * Delete segments.
	 * 
	 * @param layer Layer
	 * @param from  Delete from
	 * @param to    Delete to
	 **/
	public void deleteStrSegment(final int layer, final int from, final int to) {
		final int[] fromL = new int[] {-1, -1, -1};
		final int[] toL   = new int[] {-1, -1, -1};

		final ArrayList<StrSegment> strLayer2 = mStringLayer[2];
		final ArrayList<StrSegment> strLayer1 = mStringLayer[1];

		if (layer == 2) {
			fromL[2] = from;
			toL[2]   = to;
			fromL[1] = strLayer2.get(from).from;
			toL[1]   = strLayer2.get(to).to;
			fromL[0] = strLayer1.get(fromL[1]).from;
			toL[0]   = strLayer1.get(toL[1]).to;
		} else if (layer == 1) {
			fromL[1] = from;
			toL[1]   = to;
			fromL[0] = strLayer1.get(from).from;
			toL[0]   = strLayer1.get(to).to;
		} else {
			fromL[0] = from;
			toL[0]   = to;
		}

		int diff = to - from + 1;
		for (int lv = 0; lv < MAX_LAYER; lv++) {
			if (fromL[lv] >= 0) {
				deleteStrSegment0(lv, fromL[lv], toL[lv], diff);
			} else {
				int boundary_from = -1;
				int boundary_to   = -1;
				final ArrayList<StrSegment> strLayer = mStringLayer[lv];
				for (int i = 0; i < strLayer.size(); i++) {
					final StrSegment ss = strLayer.get(i);
					if (((ss.from >= fromL[lv-1]) && (ss.from <= toL[lv-1])) ||
							((ss.to >= fromL[lv-1]) && (ss.to <= toL[lv-1])) ) {
						if (fromL[lv] < 0) {
							fromL[lv] = i;
							boundary_from = ss.from;
						}
						toL[lv] = i;
						boundary_to = ss.to;
					} else if ((ss.from <= fromL[lv-1]) && (ss.to >= toL[lv-1])) {
						boundary_from = ss.from;
						boundary_to   = ss.to;
						fromL[lv] = i;
						toL[lv] = i;
						break;
					} else if (ss.from > toL[lv-1]) {
						break;
					}
				}
				if ((boundary_from != fromL[lv-1]) || (boundary_to != toL[lv-1])) {
					deleteStrSegment0(lv, fromL[lv] + 1, toL[lv], diff);
					boundary_to -= diff;
					final StrSegment[] tmp = new StrSegment[] {
							(new StrSegment(toString(lv-1), boundary_from, boundary_to))
					};
					replaceStrSegment0(lv, tmp, fromL[lv], fromL[lv]);
					return;
				} else {
					deleteStrSegment0(lv, fromL[lv], toL[lv], diff);
				}
			}
			diff = toL[lv] - fromL[lv] + 1;
		}
	}

	/**
	 * Delete segments (internal method).
	 * 
	 * @param layer     Layer
	 * @param from      Delete from
	 * @param to        Delete to
	 * @param diff      Differential
	 **/
	private void deleteStrSegment0(final int layer, final int from, final int to, final int diff) {
		final ArrayList<StrSegment> strLayer = mStringLayer[layer];
		if (diff != 0) {
			for (int i = to + 1; i < strLayer.size(); i++) {
				final StrSegment ss = strLayer.get(i);
				ss.from -= diff;
				ss.to   -= diff;
			}
		}
		for (int i = from; i <= to; i++) {
			if (from < strLayer.size()) {
				strLayer.remove(from);
			}
		}
	}

	/**
	 * Delete a segment at the cursor.
	 * 
	 * @param layer         Layer
	 * @param rightside     {@code true} if direction is rightward at the cursor, {@code false} if direction is leftward at the cursor
	 * @return              The number of string segments in the specified layer
	 **/
	public int delete(final int layer, final boolean rightside) {
		final int cursor = mCursor[layer];
		final ArrayList<StrSegment> strLayer = mStringLayer[layer];

		if (!rightside && (cursor > 0)) {
			deleteStrSegment(layer, cursor-1, cursor-1);
			setCursor(layer, cursor - 1);
		} else if (rightside && (cursor < strLayer.size())) {
			deleteStrSegment(layer, cursor, cursor);
			setCursor(layer, cursor);
		}
		return strLayer.size();
	}

	/**
	 * Get the string layer.
	 *
	 * @param layer     Layer
	 * @return          {@link ArrayList} of {@link StrSegment}; {@code null} if error.
	 **/
	public ArrayList<StrSegment> getStringLayer(final int layer) {
		try {
			return mStringLayer[layer];
		} catch (final Exception ex) {
			return null;
		}
	}

	/**
	 * Get upper the segment which includes the position.
	 * 
	 * @param layer     Layer
	 * @param pos       Position
	 * @return      Index of upper segment
	 */
	private int included(final int layer, final int pos) {
		if (pos == 0) {
			return 0;
		}
		final int uplayer = layer + 1;
		int i;
		final ArrayList<StrSegment> strLayer = mStringLayer[uplayer];
		for (i = 0; i < strLayer.size(); i++) {
			final StrSegment ss = strLayer.get(i);
			if ((ss.from <= pos) && (pos <= ss.to)) {
				break;
			}
		}
		return i;
	}

	/**
	 * Set the cursor.
	 * 
	 * @param layer     Layer
	 * @param pos       Position of cursor
	 * @return      New position of cursor
	 */
	public int setCursor(final int layer, int pos) {
		if (pos > mStringLayer[layer].size()) {
			pos = mStringLayer[layer].size();
		}
		if (pos < 0) {
			pos = 0;
		}
		if (layer == 0) {
			mCursor[0] = pos;
			mCursor[1] = included(0, pos);
			mCursor[2] = included(1, mCursor[1]);
		} else if (layer == 1) {
			mCursor[2] = included(1, pos);
			mCursor[1] = pos;
			final int pos1 = (pos < mStringLayer[1].size()) ? pos : mStringLayer[1].size();
			mCursor[0] = (pos > 0)? mStringLayer[1].get(pos1 - 1).to+1 : 0;
		} else {
			mCursor[2] = pos;
			final int pos2 = (pos < mStringLayer[2].size()) ? pos : mStringLayer[2].size();
			mCursor[1] = (pos > 0)? mStringLayer[2].get(pos2 - 1).to+1 : 0;
			final int pos1 = (mCursor[1] < mStringLayer[1].size()) ? mCursor[1] : mStringLayer[2].size();
			mCursor[0] = (mCursor[1] > 0)? mStringLayer[1].get(pos1 - 1).to+1 : 0;
		}
		return pos;
	}

	/**
	 * Set the cursor.
	 * 
	 * @param layer     Layer
	 * @param pos       Position of cursor
	 */
	public void setCursorDirect(int layer, int pos) {
		mCursor[layer] = pos;
	}

	/**
	 * Move the cursor.
	 *
	 * @param layer     Layer
	 * @param diff      Relative position from current cursor position
	 * @return      New position of cursor
	 **/
	public int moveCursor(final int layer, final int diff) {
		final int c = mCursor[layer] + diff;

		return setCursor(layer, c);
	}

	/**
	 * Get the cursor position.
	 *
	 * @param layer     Layer
	 * @return cursor   Current position of cursor
	 **/
	public int getCursor(final int layer) {
		return mCursor[layer];
	}

	/**
	 * Get the number of segments.
	 *
	 * @param layer     Layer
	 * @return          Number of segments
	 **/
	public int size(final int layer) {
		return mStringLayer[layer].size();
	}

	/**
	 * Clear all information.
	 */
	public void clear() {
		for (int i = 0; i < MAX_LAYER; i++) {
			mStringLayer[i].clear();
			mCursor[i] = 0;
		}
	}
}
