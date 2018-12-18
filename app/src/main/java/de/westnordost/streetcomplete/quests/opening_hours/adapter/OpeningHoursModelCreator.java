package de.westnordost.streetcomplete.quests.opening_hours.adapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.westnordost.streetcomplete.quests.opening_hours.model.OpeningMonths;
import de.westnordost.streetcomplete.quests.opening_hours.model.OpeningWeekdays;
import de.westnordost.streetcomplete.quests.opening_hours.model.TimeRange;

/** Transforms the adapter's view model to the model */
public class OpeningHoursModelCreator
{
	public static List<OpeningMonths> create(List<OpeningMonthsRow> viewData)
	{
		return createOpeningMonthsList(viewData);
	}

	private static List<OpeningMonths> createOpeningMonthsList(List<OpeningMonthsRow> openingMonthsList)
	{
		List<OpeningMonths> result = new ArrayList<>();
		for (OpeningMonthsRow openingMonths : openingMonthsList)
		{
			result.add(createOpeningMonths(openingMonths));
		}
		return result;
	}

	private static OpeningMonths createOpeningMonths(OpeningMonthsRow openingMonthsRow)
	{
		List<OpeningWeekdays> weekdaysList = createOpeningWeekdays(openingMonthsRow.getWeekdaysList());
		List<List<OpeningWeekdays>> clusters = createWeekdaysClusters(weekdaysList);

		return new OpeningMonths(openingMonthsRow.getMonths(), clusters);
	}

	private static List<OpeningWeekdays> createOpeningWeekdays(List<OpeningWeekdaysRow> openingWeekdaysRows)
	{
		List<OpeningWeekdays> result = new ArrayList<>();
		OpeningWeekdays last = null;
		for (OpeningWeekdaysRow row : openingWeekdaysRows)
		{
			// merging rows that have the same weekdays
			if(last != null && last.getWeekdays().equals(row.getWeekdays()))
			{
				last.getTimeRanges().add(row.getTimeRange());
			}
			else
			{
				ArrayList<TimeRange> times = new ArrayList<>(1);
				times.add(row.getTimeRange());
				last = new OpeningWeekdays(row.getWeekdays(), times);
				result.add(last);
			}
		}
		return result;
	}

	private static List<List<OpeningWeekdays>> createWeekdaysClusters(List<OpeningWeekdays> list) {
		ArrayList<OpeningWeekdays> unsorted = new ArrayList<>(list);

		List<List<OpeningWeekdays>> clusters = new ArrayList<>();

		while(!unsorted.isEmpty())
		{
			ArrayList<OpeningWeekdays> cluster = new ArrayList<>();
			cluster.add(unsorted.remove(0));
			Iterator<OpeningWeekdays> it = unsorted.iterator();
			while (it.hasNext())
			{
				OpeningWeekdays other = it.next();
				boolean anyWeekdaysOverlap = false;
				boolean anyTimesOverlap = false;
				for (OpeningWeekdays inThisCluster : cluster)
				{
					boolean weekdaysOverlaps = inThisCluster.intersectsWeekdays(other);
					boolean anyTimeRangeOverlaps = inThisCluster.intersects(other);
					anyTimesOverlap |= weekdaysOverlaps && anyTimeRangeOverlaps;
					anyWeekdaysOverlap |= weekdaysOverlaps;
				}
				if (anyWeekdaysOverlap && !anyTimesOverlap)
				{
					cluster.add(other);
					it.remove();
				}
			}
			clusters.add(cluster);
		}

		return clusters;
	}
}
