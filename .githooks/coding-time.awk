# Turns a sorted list of commit timestamps into the card drawn at the top of the
# README, and into the markdown block that points at it.
#
# Driven by coding-time, which supplies every variable below with -v. Reads
# one Unix timestamp per line, oldest first.
#
#   gap         seconds that end a sitting
#   opening     minutes credited to the commit that opens one
#   recent      seconds counted as "recent"
#   days        how many days of chart to draw at most
#   now         seconds since the epoch, taken once by the caller
#   first_seen  date of the oldest commit, already formatted
#   last_seen   date of the newest commit, already formatted
#   card        file to write the SVG to
#   block       file to write the markdown to

function hours(minutes) {
	return sprintf("%.1f", minutes / 60)
}

# Roughly how wide a string runs at a given font size in the card's font stack.
# SVG cannot measure text, so the card is sized from an estimate rather than
# from the glyphs themselves.
function width_of(text, size) {
	return length(text) * size * 0.52
}

function svg(line) {
	print line > card
}

function markdown(line) {
	print line > block
}

{
	# Whether this commit opened a sitting is a fact about the gap, not about the
	# minutes it earned: a real gap of exactly `opening` minutes would otherwise
	# be mistaken for one.
	opened = (commits == 0 || $1 - previous > gap)
	minutes = opened ? opening : ($1 - previous) / 60
	if (opened) {
		sittings++
	}
	total += minutes
	if (now - $1 <= recent) {
		recently += minutes
	}

	# Days counted back from today, so the rightmost bar is the current one. A
	# commit dated in the future — a skewed clock, a hand-set date — would count
	# backwards from today, so it is pinned to today instead.
	day = int((now - $1) / 86400)
	if (day < 0) {
		day = 0
	}
	if (day < days) {
		by_day[day] += minutes
		if (by_day[day] > tallest) {
			tallest = by_day[day]
		}
	}
	if (day + 1 > lived) {
		lived = day + 1
	}

	previous = $1
	commits++
}

END {
	if (commits == 0) {
		exit
	}

	# One bar per day the repository has existed, up to the window asked for, so
	# a young project is not padded out with days that predate it.
	if (lived < days) {
		days = lived
	}
	if (days < 1) {
		days = 1
	}

	headline = hours(total)
	summary = hours(recently) " hrs in the last " int(recent / 86400) " days   \302\267   " \
			sittings " sittings   \302\267   " commits " commits"

	PAD = 24
	BASE = 130
	CEILING = 46
	HEADLINE_SIZE = 38
	SUMMARY_SIZE = 12
	HEIGHT = 164
	MINIMUM_BAR = 3

	# Width follows the text, and the chart is then stretched across whatever that
	# leaves, so the card reads the same at any number of days.
	width = int(PAD + width_of(headline, HEADLINE_SIZE) + 46 + width_of(summary, SUMMARY_SIZE) + PAD)
	span = width - PAD * 2
	# A bar per day, each taking about two thirds of its slot: thin enough to read
	# as a chart rather than a row of blocks, and it stays that way as days pile up.
	pitch = span / days
	bar = int(pitch * 0.68)
	if (bar < 2) {
		bar = 2
	}

	printf "" > card
	svg("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" width "\" height=\"" HEIGHT "\"" \
			" viewBox=\"0 0 " width " " HEIGHT "\" role=\"img\"" \
			" aria-label=\"Time on record: " headline " hours over " commits " commits\">")
	svg("  <style>")
	svg("    .card { fill: #0d1117; stroke: #30363d }")
	svg("    text { font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Helvetica, Arial, sans-serif }")
	svg("    .label { font-size: 11px; letter-spacing: 1.7px; fill: #8b949e }")
	svg("    .figure { font-size: " HEADLINE_SIZE "px; font-weight: 600; fill: #e6edf3 }")
	svg("    .unit { font-size: 13px; fill: #8b949e }")
	svg("    .note { font-size: " SUMMARY_SIZE "px; fill: #8b949e }")
	svg("    .axis { font-size: 10px; fill: #6e7681 }")
	svg("    .bar { fill: #388bfd }")
	svg("    .bar-quiet { fill: #21262d }")
	svg("  </style>")
	svg("  <rect class=\"card\" x=\"0.5\" y=\"0.5\" width=\"" (width - 1) "\" height=\"" (HEIGHT - 1) "\" rx=\"8\"/>")
	svg("  <text class=\"label\" x=\"" PAD "\" y=\"36\">TIME ON RECORD</text>")
	svg("  <text class=\"figure\" x=\"" PAD "\" y=\"78\">" headline "<tspan class=\"unit\" dx=\"7\">hrs</tspan></text>")
	svg("  <text class=\"note\" x=\"" (width - PAD) "\" y=\"78\" text-anchor=\"end\">" summary "</text>")

	for (i = 0; i < days; i++) {
		minutes = by_day[days - 1 - i] + 0
		height = (tallest > 0) ? minutes / tallest * CEILING : 0
		# A day too quiet to draw still gets a stub, so the empty days read as an
		# axis rather than as a gap in the chart.
		if (height < MINIMUM_BAR) {
			height = MINIMUM_BAR
			style = (minutes > 0) ? "bar" : "bar-quiet"
		}
		else {
			style = "bar"
		}
		# SVG measures y downwards, so the top of a bar is the baseline minus its height.
		svg(sprintf("  <rect class=\"%s\" x=\"%.1f\" y=\"%.1f\" width=\"%d\" height=\"%.1f\" rx=\"1.5\"/>",
				style, PAD + i * pitch, BASE - height, bar, height))
	}

	svg("  <text class=\"axis\" x=\"" PAD "\" y=\"148\">" first_seen "</text>")
	svg("  <text class=\"axis\" x=\"" (width / 2) "\" y=\"148\" text-anchor=\"middle\">" \
			days (days == 1 ? " day" : " days") "</text>")
	svg("  <text class=\"axis\" x=\"" (width - PAD) "\" y=\"148\" text-anchor=\"end\">" last_seen "</text>")
	svg("</svg>")
	close(card)

	printf "" > block
	markdown("")
	markdown("![Time on record](.idea/readme/image/time-on-record.svg)")
	markdown("")
	markdown("<details>")
	markdown("<summary>How this is counted</summary>")
	markdown("")
	markdown("Commits record when work was saved, never how long it took, so this is an")
	markdown("estimate rather than a timesheet. Commits less than " int(gap / 60) " minutes apart")
	markdown("count as one sitting and contribute the real time between them; a commit that")
	markdown("opens a sitting contributes a flat " opening " minutes for the work that led up to")
	markdown("it. Merges are skipped, and nothing that was never committed is visible here.")
	markdown("")
	markdown("Covers every author. Regenerated on each commit by `.githooks/coding-time`,")
	markdown("which reads commit timestamps and nothing else. `GAP_MINUTES`, `OPENING_MINUTES`,")
	markdown("`RECENT_DAYS` and `DAYS` change what it assumes.")
	markdown("")
	markdown("</details>")
	markdown("")
	close(block)
}
