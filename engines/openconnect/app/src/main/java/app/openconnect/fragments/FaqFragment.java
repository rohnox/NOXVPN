/*
 * Adapted from OpenVPN for Android
 * Copyright (c) 2012-2013, Arne Schwabe
 * Copyright (c) 2014, Kevin Cernekee
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * In addition, as a special exception, the copyright holders give
 * permission to link the code of portions of this program with the
 * OpenSSL library.
 */

package app.openconnect.fragments;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.openconnect.R;

public class FaqFragment extends Fragment  {

	private String htmlEncode(String in) {
		in = TextUtils.htmlEncode(in).replace("\n", "<br>");

		// match markdown-formatted links: [link text](http://foo.bar.com)
		// replace with: <a href="http://foo.bar.com">link text</a>
		StringBuilder out = new StringBuilder();
		Pattern p = Pattern.compile("\\[(.+?)\\]\\((\\S+)\\)");
		Matcher m;

		while (true) {
			m = p.matcher(in);
			if (!m.find()) {
				break;
			}
			out.append(in.substring(0, m.start()));
			out.append("<a href=\"" + m.group(2) + "\">");
			out.append(m.group(1));
			out.append("</a>");
			in = in.substring(m.end());
		}

		out.append(in);
		return out.toString();
	}

	private int dp(Activity act, float value) {
		float scale = act.getResources().getDisplayMetrics().density;
		return (int)(value * scale + 0.5f);
	}

	private TextView newText(Activity act, CharSequence text, float sp, int colorAttr) {
		TextView tv = new TextView(act);
		tv.setText(text);
		tv.setTextSize(sp);
		tv.setTextColor(resolveColor(act, colorAttr));
		tv.setLineSpacing(dp(act, 2), 1.0f);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		return tv;
	}

	private int resolveColor(Activity act, int attr) {
		android.util.TypedValue out = new android.util.TypedValue();
		if (act.getTheme().resolveAttribute(attr, out, true)) {
			if (out.resourceId != 0) {
				return act.getResources().getColor(out.resourceId, act.getTheme());
			}
			return out.data;
		}
		return 0xff000000;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.faq, container, false);
		Activity act = getActivity();

		String items[] = getResources().getStringArray(R.array.faq_text);
		LinearLayout contents = (LinearLayout)v.findViewById(R.id.faq_container);

		for (int i = 0; i < items.length; i += 2) {
			LinearLayout card = new LinearLayout(act);
			card.setOrientation(LinearLayout.VERTICAL);
			card.setPadding(dp(act, 16), dp(act, 14), dp(act, 16), dp(act, 14));
			android.util.TypedValue selectable = new android.util.TypedValue();
			act.getTheme().resolveAttribute(
					android.R.attr.selectableItemBackground, selectable, true);
			if (selectable.resourceId != 0) {
				card.setBackgroundResource(selectable.resourceId);
			}

			LinearLayout header = new LinearLayout(act);
			header.setOrientation(LinearLayout.HORIZONTAL);
			header.setGravity(android.view.Gravity.CENTER_VERTICAL);

			TextView question = newText(act, items[i], 15,
					com.google.android.material.R.attr.colorOnSurface);
			question.setTypeface(null, android.graphics.Typeface.BOLD);
			question.setMovementMethod(null);
			LinearLayout.LayoutParams questionParams = new LinearLayout.LayoutParams(
					0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
			question.setLayoutParams(questionParams);
			header.addView(question);

			ImageView expand = new ImageView(act);
			expand.setImageResource(R.drawable.ic_expand_more_24);
			expand.setColorFilter(resolveColor(act,
					com.google.android.material.R.attr.colorOnSurfaceVariant));
			expand.setContentDescription(getString(R.string.expand_answer));
			LinearLayout.LayoutParams expandParams = new LinearLayout.LayoutParams(
					dp(act, 28), dp(act, 28));
			expandParams.setMargins(dp(act, 12), 0, 0, 0);
			expand.setLayoutParams(expandParams);
			header.addView(expand);
			card.addView(header);

			TextView answer = newText(act, Html.fromHtml(htmlEncode(items[i + 1])), 14,
					com.google.android.material.R.attr.colorOnSurfaceVariant);
			LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			answerParams.setMargins(0, dp(act, 8), 0, 0);
			answer.setLayoutParams(answerParams);
			answer.setVisibility(View.GONE);
			card.addView(answer);

			card.setClickable(true);
			card.setFocusable(true);
			card.setOnClickListener(clicked -> {
				boolean expanding = answer.getVisibility() != View.VISIBLE;
				answer.setVisibility(expanding ? View.VISIBLE : View.GONE);
				expand.animate().rotation(expanding ? 180f : 0f).setDuration(160).start();
				expand.setContentDescription(getString(
						expanding ? R.string.collapse_answer : R.string.expand_answer));
			});

			contents.addView(card);
			if (i + 2 < items.length) {
				View divider = new View(act);
				divider.setBackgroundColor(resolveColor(act,
						com.google.android.material.R.attr.colorOutlineVariant));
				LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, dp(act, 1));
				dividerParams.setMargins(dp(act, 16), 0, dp(act, 16), 0);
				divider.setLayoutParams(dividerParams);
				contents.addView(divider);
			}
		}

		return v;
	}
}
