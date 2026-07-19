from pathlib import Path

source = Path('/tmp/elektromeister-src/ElektroMeisterV2_clean/app/src/main/java/com/jugoconsulting/elektromojster/MainActivity.java')
text = source.read_text()

old_world = '''            for (Connection connection : connections) drawConnection(c, connection);
            if (activeTerminal != null) {
                stroke.setColor(activeTerminal.color);
                stroke.setStrokeWidth(dp(5) / scale);
                stroke.setAlpha(210);
                c.drawLine(activeTerminal.x, activeTerminal.y, dragWorldX, dragWorldY, stroke);
                stroke.setAlpha(255);
            }

            for (GroupBox box : groupBoxes) {
                paint.setColor(PANEL_LIGHT);
                c.drawRoundRect(box.rect, dp(18), dp(18), paint);
                stroke.setColor(Color.rgb(105, 124, 137));
                stroke.setStrokeWidth(dp(1.5f));
                c.drawRoundRect(box.rect, dp(18), dp(18), stroke);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(sp(14));
                paint.setColor(MUTED);
                c.drawText(box.name, box.rect.centerX(), box.rect.top + dp(33), paint);
            }

            for (Terminal terminal : terminals) drawTerminal(c, terminal);
'''

new_world = '''            for (GroupBox box : groupBoxes) {
                paint.setColor(PANEL_LIGHT);
                c.drawRoundRect(box.rect, dp(18), dp(18), paint);
                stroke.setColor(Color.rgb(105, 124, 137));
                stroke.setStrokeWidth(dp(1.5f));
                c.drawRoundRect(box.rect, dp(18), dp(18), stroke);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(sp(14));
                paint.setColor(MUTED);
                c.drawText(box.name, box.rect.centerX(), box.rect.top + dp(33), paint);
            }

            // Kabel werden vor den Bauteilen gezeichnet und bleiben vollständig sichtbar.
            for (Connection connection : connections) drawConnection(c, connection);
            if (activeTerminal != null) {
                drawCableLine(c, activeTerminal.x, activeTerminal.y, dragWorldX, dragWorldY,
                        activeTerminal.color, dp(5) / scale, 210);
            }

            for (Terminal terminal : terminals) drawTerminal(c, terminal);
'''

old_connection = '''        private void drawConnection(Canvas c, Connection connection) {
            Terminal a = terminalById(connection.a);
            Terminal b = terminalById(connection.b);
            if (a == null || b == null) return;
            Path path = new Path();
            path.moveTo(a.x, a.y);
            float midX = (a.x + b.x) / 2f;
            path.cubicTo(midX, a.y, midX, b.y, b.x, b.y);
            stroke.setColor(a.color);
            stroke.setStrokeWidth(dp(5));
            stroke.setShadowLayer(dp(4), 0, dp(2), Color.argb(100, 0, 0, 0));
            c.drawPath(path, stroke);
            stroke.clearShadowLayer();
        }
'''

new_connection = '''        private void drawConnection(Canvas c, Connection connection) {
            Terminal a = terminalById(connection.a);
            Terminal b = terminalById(connection.b);
            if (a == null || b == null) return;
            Path path = new Path();
            path.moveTo(a.x, a.y);
            float midX = (a.x + b.x) / 2f;
            path.cubicTo(midX, a.y, midX, b.y, b.x, b.y);

            // Schwarze und sehr dunkle Leitungen erhalten eine helle Kontur.
            if (isDarkCable(a.color)) {
                stroke.setColor(Color.rgb(220, 228, 234));
                stroke.setStrokeWidth(dp(9));
                stroke.setAlpha(235);
                c.drawPath(path, stroke);
            }

            stroke.setColor(a.color);
            stroke.setStrokeWidth(dp(5));
            stroke.setAlpha(255);
            stroke.setShadowLayer(dp(4), 0, dp(2), Color.argb(100, 0, 0, 0));
            c.drawPath(path, stroke);
            stroke.clearShadowLayer();
        }

        private void drawCableLine(Canvas c, float x1, float y1, float x2, float y2,
                                   int color, float width, int alpha) {
            if (isDarkCable(color)) {
                stroke.setColor(Color.rgb(220, 228, 234));
                stroke.setStrokeWidth(width + dp(4));
                stroke.setAlpha(Math.min(255, alpha + 25));
                c.drawLine(x1, y1, x2, y2, stroke);
            }
            stroke.setColor(color);
            stroke.setStrokeWidth(width);
            stroke.setAlpha(alpha);
            c.drawLine(x1, y1, x2, y2, stroke);
            stroke.setAlpha(255);
        }

        private boolean isDarkCable(int color) {
            double luminance = 0.2126 * Color.red(color)
                    + 0.7152 * Color.green(color)
                    + 0.0722 * Color.blue(color);
            return luminance < 72;
        }
'''

if old_world not in text:
    raise SystemExit('drawWorld block not found')
if old_connection not in text:
    raise SystemExit('drawConnection block not found')

text = text.replace(old_world, new_world).replace(old_connection, new_connection)
source.write_text(text)

gradle = Path('/tmp/elektromeister-src/ElektroMeisterV2_clean/app/build.gradle')
g = gradle.read_text().replace('versionCode 2', 'versionCode 3')
g = g.replace("versionName '2.0.0'", "versionName '2.1.0'")
gradle.write_text(g)
