package de.westnordost.streetcomplete.quests.opening_hours.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.westnordost.streetcomplete.R;
import de.westnordost.streetcomplete.data.meta.CountryInfo;
import de.westnordost.streetcomplete.quests.opening_hours.model.CircularSection;
import de.westnordost.streetcomplete.quests.opening_hours.model.NumberSystem;
import de.westnordost.streetcomplete.quests.opening_hours.model.OpeningMonths;
import de.westnordost.streetcomplete.quests.opening_hours.model.TimeRange;
import de.westnordost.streetcomplete.quests.opening_hours.TimeRangePickerDialog;
import de.westnordost.streetcomplete.quests.opening_hours.model.Weekdays;
import de.westnordost.streetcomplete.quests.opening_hours.WeekdaysPickerDialog;
import de.westnordost.streetcomplete.view.dialogs.RangePickerDialog;

public class AddOpeningHoursAdapter extends RecyclerView.Adapter
{
	private final static int MONTHS = 0, WEEKDAYS = 1;

	private ArrayList<OpeningMonthsRow> viewData;
	private final Context context;
	private final CountryInfo countryInfo;
	private boolean displayMonths = false;

	public AddOpeningHoursAdapter(ArrayList<OpeningMonthsRow> viewData, Context context, CountryInfo countryInfo)
	{
		this.viewData = viewData;
		this.context = context;
		this.countryInfo = countryInfo;
	}

	@NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		switch (viewType)
		{
			case MONTHS:
				return new MonthsViewHolder(
						inflater.inflate(R.layout.quest_times_month_row, parent, false));
			case WEEKDAYS:
				return new WeekdayViewHolder(
						inflater.inflate(R.layout.quest_times_weekday_row, parent, false));
		}
		throw new IllegalArgumentException("Unknown viewType " + viewType);
	}

	@Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
	{
		int[] p = getHierarchicPosition(position);
		OpeningMonthsRow om = viewData.get(p[0]);

		if(holder instanceof MonthsViewHolder)
		{
			((MonthsViewHolder) holder).update(om);
		}
		else if(holder instanceof WeekdayViewHolder)
		{
			OpeningWeekdaysRow ow = om.weekdaysList.get(p[1]);
			OpeningWeekdaysRow prevOw = null;
			if(p[1] > 0) prevOw = om.weekdaysList.get(p[1] - 1);
			((WeekdayViewHolder) holder).update(ow, prevOw);
		}
	}

	@Override public int getItemViewType(int position)
	{
		int[] p = getHierarchicPosition(position);
		return p.length == 1 ? MONTHS : WEEKDAYS;
	}

	private int[] getHierarchicPosition(int position)
	{
		int count = 0;
		for (int i = 0; i < viewData.size(); ++i)
		{
			OpeningMonthsRow om = viewData.get(i);
			if(count == position) return new int[]{i};
			++count;

			for (int j = 0; j < om.weekdaysList.size(); ++j)
			{
				if(count == position) return new int[]{i,j};
				++count;
			}
		}
		throw new IllegalArgumentException();
	}

	@Override public int getItemCount()
	{
		int count = 0;
		for (OpeningMonthsRow om : viewData)
		{
			count += om.weekdaysList.size();
		}
		count += viewData.size();
		return count;
	}

	/* ------------------------------------------------------------------------------------------ */

	private void remove(int position)
	{
		int[] p = getHierarchicPosition(position);
		if (p.length != 2) throw new IllegalArgumentException("May only directly remove weekdays, not months");

		ArrayList<OpeningWeekdaysRow> weekdays = viewData.get(p[0]).weekdaysList;
		weekdays.remove(p[1]);
		notifyItemRemoved(position);
		// if not last weekday removed -> element after this one may need to be updated
		// because it may need to show the weekdays now
		if(p[1] < weekdays.size()) notifyItemChanged(position);
		// if no weekdays left in months: remove/reset months
		if(weekdays.isEmpty())
		{
			if(viewData.size() == 1)
			{
				viewData.set(0, new OpeningMonthsRow());
				setDisplayMonths(false);
				notifyItemChanged(0);
			}
			else
			{
				viewData.remove(p[0]);
				notifyItemRemoved(position-1);
			}
		}
	}

	public void addNewMonths()
	{
		openSetMonthsRangeDialog(getMonthsRangeSuggestion(), (startIndex, endIndex) ->
		{
			final CircularSection months = new CircularSection(startIndex, endIndex);
			openSetWeekdaysDialog(getWeekdaysSuggestion(true), weekdays ->
			{
				openSetTimeRangeDialog(getOpeningHoursSuggestion(), timeRange ->
					addMonths(months, weekdays, timeRange));
			});
		});
	}

	private void addMonths(CircularSection months, Weekdays weekdays, TimeRange timeRange)
	{
		int insertIndex = getItemCount();
		viewData.add(new OpeningMonthsRow(months, new OpeningWeekdaysRow(weekdays, timeRange)));
		notifyItemRangeInserted(insertIndex, 2); // 2 = opening month + opening weekday
	}

	public void addNewWeekdays()
	{
		boolean isFirst = viewData.get(viewData.size()-1).weekdaysList.isEmpty();
		openSetWeekdaysDialog(getWeekdaysSuggestion(isFirst), weekdays ->
		{
			openSetTimeRangeDialog(getOpeningHoursSuggestion(),
					timeRange -> addWeekdays(weekdays, timeRange));
		});
	}

	private void addWeekdays(Weekdays weekdays, TimeRange timeRange)
	{
		int insertIndex = getItemCount();
		viewData.get(viewData.size()-1).weekdaysList.add(new OpeningWeekdaysRow(weekdays, timeRange));
		notifyItemInserted(insertIndex);
	}

	public ArrayList<OpeningMonthsRow> getViewData()
	{
		return viewData;
	}

	public List<OpeningMonths> createData()
	{
		return OpeningHoursModelCreator.create(viewData);
	}

	public void setDisplayMonths(boolean displayMonths)
	{
		this.displayMonths = displayMonths;
		notifyDataSetChanged();
	}

	public boolean isDisplayMonths()
	{
		return displayMonths;
	}

	public void changeToMonthsMode()
	{
		final OpeningMonthsRow om = viewData.get(0);
		openSetMonthsRangeDialog(om.months, (startIndex, endIndex) ->
		{
			if(om.weekdaysList.isEmpty())
			{
				openSetWeekdaysDialog(getWeekdaysSuggestion(true), weekdays ->
				{
					openSetTimeRangeDialog(getOpeningHoursSuggestion(), timeRange ->
					{
						changedToMonthsMode(startIndex, endIndex);
						om.weekdaysList.add(new OpeningWeekdaysRow(weekdays, timeRange));
						notifyItemInserted(1);
					});
				});
			}
			else
			{
				changedToMonthsMode(startIndex, endIndex);
			}
		});
	}

	private void changedToMonthsMode(int startIndex, int endIndex)
	{
		setDisplayMonths(true);
		viewData.get(0).months = new CircularSection(startIndex, endIndex);
		notifyItemChanged(0);
	}

	/* -------------------------------------- months select --------------------------------------*/

	private class MonthsViewHolder extends RecyclerView.ViewHolder
	{
		private TextView monthsText;
		private View delete;

		public MonthsViewHolder(View itemView)
		{
			super(itemView);
			monthsText = itemView.findViewById(R.id.monthsLabel);
			delete = itemView.findViewById(R.id.deleteButton);
			delete.setVisibility(View.GONE);
		}

		public void setVisibility(boolean isVisible)
		{
			RecyclerView.LayoutParams param = (RecyclerView.LayoutParams)itemView.getLayoutParams();
			if (isVisible)
			{
				param.height = LinearLayout.LayoutParams.WRAP_CONTENT;
				param.width = LinearLayout.LayoutParams.MATCH_PARENT;
				itemView.setVisibility(View.VISIBLE);
			}
			else
			{
				itemView.setVisibility(View.GONE);
				param.height = 0;
				param.width = 0;
			}
			itemView.setLayoutParams(param);
		}

		public void update(final OpeningMonthsRow data)
		{
			setVisibility(displayMonths);
			monthsText.setText(data.months.toStringUsing(DateFormatSymbols.getInstance().getMonths(), "–"));
			monthsText.setOnClickListener(v ->
			{
				openSetMonthsRangeDialog(data.months, (startIndex, endIndex) ->
				{
					data.months = new CircularSection(startIndex, endIndex);
					notifyItemChanged(getAdapterPosition());
				});
			});
		}
	}

	private @NonNull CircularSection getMonthsRangeSuggestion()
	{
		List<CircularSection> months = getUnmentionedMonths();
		if(months.isEmpty())
		{
			return new CircularSection(0, OpeningMonths.MAX_MONTH_INDEX);
		}
		return months.get(0);
	}

	private List<CircularSection> getUnmentionedMonths()
	{
		List<CircularSection> allTheMonths = new ArrayList<>();
		for (OpeningMonthsRow om : viewData)
		{
			allTheMonths.add(om.months);
		}
		return new NumberSystem(0,OpeningMonths.MAX_MONTH_INDEX).complemented(allTheMonths);
	}

	private void openSetMonthsRangeDialog(CircularSection months,
										  RangePickerDialog.OnRangeChangeListener callback )
	{
		String[] monthNames = DateFormatSymbols.getInstance().getMonths();
		String selectMonths = context.getResources().getString(R.string.quest_openingHours_chooseMonthsTitle);
		new RangePickerDialog(context, callback, monthNames, months.getStart(),
				months.getEnd(), selectMonths).show();
	}

	/* ------------------------------------ weekdays select --------------------------------------*/

	private class WeekdayViewHolder extends RecyclerView.ViewHolder
	{
		private TextView weekdaysText;
		private TextView hoursText;
		private View delete;

		public WeekdayViewHolder(View itemView)
		{
			super(itemView);
			weekdaysText = itemView.findViewById(R.id.weekdaysLabel);
			hoursText = itemView.findViewById(R.id.hoursLabel);
			delete = itemView.findViewById(R.id.deleteButton);
			delete.setOnClickListener(v ->
			{
				int index = getAdapterPosition();
				if(index != RecyclerView.NO_POSITION) remove(getAdapterPosition());
			});
		}

		public void update(final OpeningWeekdaysRow data, final OpeningWeekdaysRow previousData)
		{
			if(previousData != null && data.weekdays.equals(previousData.weekdays))
			{
				weekdaysText.setText("");
			}
			else
			{
				weekdaysText.setText(data.weekdays.toLocalizedString(context.getResources()));
			}

			weekdaysText.setOnClickListener(v ->
			{
				openSetWeekdaysDialog(data.weekdays, weekdays ->
				{
					data.weekdays = weekdays;
					notifyItemChanged(getAdapterPosition());
				});
			});
			hoursText.setText(data.timeRange.toStringUsing(Locale.getDefault(), "–"));
			hoursText.setOnClickListener(v ->
			{
				openSetTimeRangeDialog(data.timeRange, timeRange ->
				{
					data.timeRange = timeRange;
					notifyItemChanged(getAdapterPosition());
				});
			});
		}
	}

	private @NonNull Weekdays getWeekdaysSuggestion(boolean isFirst)
	{
		if(isFirst)
		{
			int firstWorkDayIdx = Weekdays.Companion.getWeekdayIndex(countryInfo.getFirstDayOfWorkweek());
			boolean[] result = new boolean[7];
			for(int i = 0; i < countryInfo.getRegularShoppingDays(); ++i)
			{
				result[(i + firstWorkDayIdx) % 7] = true;
			}
			return new Weekdays(result);
		}
		return new Weekdays();
	}

	private void openSetWeekdaysDialog(Weekdays weekdays, WeekdaysPickerDialog.OnWeekdaysPickedListener callback)
	{
		WeekdaysPickerDialog.INSTANCE.show(context, weekdays, callback);
	}

	/* ------------------------------------- times select ----------------------------------------*/

	// this could go into per-country localization when this page (or any other source)
	// https://en.wikipedia.org/wiki/Shopping_hours contains more information about typical
	// opening hours per country
	private static final TimeRange TYPICAL_OPENING_TIMES = new TimeRange(8 * 60, 18 * 60, false);

	private void openSetTimeRangeDialog(TimeRange timeRange,
										TimeRangePickerDialog.OnTimeRangeChangeListener callback)
	{
		String startLabel = context.getResources().getString(R.string.quest_openingHours_start_time);
		String endLabel = context.getResources().getString(R.string.quest_openingHours_end_time);

		new TimeRangePickerDialog(context, startLabel, endLabel, timeRange, callback).show();
	}

	private @NonNull TimeRange getOpeningHoursSuggestion()
	{
		return TYPICAL_OPENING_TIMES;
	}
}
