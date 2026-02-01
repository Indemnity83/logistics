#!/usr/bin/env python3
"""Generate a console-style scrolling dot display texture for the laser quarry."""

from PIL import Image
import random

# Frame dimensions
FRAME_WIDTH = 16
FRAME_HEIGHT = 16

# Display area within each frame (where dots appear)
DISPLAY_X = 2       # Inset 2px from left
DISPLAY_Y = 3       # Inset 3px from top
DISPLAY_W = 12      # Visible width (model crops 2px each side, so 12px visible)
DISPLAY_H = 6       # 6px tall

# Colors
BG_COLOR = (0, 0, 0, 0)  # Transparent
DOT_COLOR = (46, 204, 113, 255)  # Green
DIM_COLOR = (39, 174, 96, 200)   # Dimmer green for older lines

# Line spacing (1px line + 1px gap = 2px per line)
LINE_HEIGHT = 2
MAX_LINES = DISPLAY_H // LINE_HEIGHT  # 3 lines visible

def generate_word():
    """Generate a random 'word' (1-3 pixels wide)."""
    return random.randint(1, 3)

def generate_line():
    """Generate a line of 'words' that fits in DISPLAY_W."""
    words = []
    x = 0
    while x < DISPLAY_W - 1:
        word_len = generate_word()
        if x + word_len > DISPLAY_W:
            break
        words.append(word_len)
        x += word_len + 1  # word + space
    return words

def draw_line(img, words, y, x_offset=0, color=DOT_COLOR):
    """Draw a line of dot-words at the given y position."""
    x = DISPLAY_X + x_offset
    for word_len in words:
        for i in range(word_len):
            if x + i < DISPLAY_X + DISPLAY_W:
                img.putpixel((x + i, y), color)
        x += word_len + 1  # word + space

def draw_cursor(img, x, y):
    """Draw a 1px cursor."""
    if DISPLAY_X <= x < DISPLAY_X + DISPLAY_W:
        img.putpixel((x, y), DOT_COLOR)

def create_frame():
    """Create an empty frame."""
    return Image.new('RGBA', (FRAME_WIDTH, FRAME_HEIGHT), BG_COLOR)

def generate_console_animation():
    """Generate the console animation frames."""
    frames = []
    random.seed(42)  # Reproducible randomness

    # Script: list of (is_command, line_words)
    # Commands have a 2px "prompt" prefix, responses don't
    script = []

    # Generate several command/response cycles
    for _ in range(4):
        # Command (shorter, like ">RUN")
        cmd_words = [2]  # prompt
        remaining = DISPLAY_W - 3  # after prompt + space
        while remaining > 1:
            w = min(generate_word(), remaining)
            cmd_words.append(w)
            remaining -= w + 1
        script.append(('cmd', cmd_words))

        # Responses (2-4 lines)
        for _ in range(random.randint(2, 4)):
            script.append(('resp', generate_line()))

    # State: visible lines (each is (words, is_old))
    # Pre-populate with some lines so animation doesn't start empty
    visible_lines = [
        (generate_line(), True),
        (generate_line(), True),
    ]

    for line_type, words in script:
        if line_type == 'cmd':
            # Typing effect: show prompt, then each word appears
            prompt = [words[0]]  # Just the prompt (2px)
            rest = words[1:]

            # Frame with just prompt + cursor
            frame = create_frame()
            y = DISPLAY_Y + (len(visible_lines) % MAX_LINES) * LINE_HEIGHT

            # Draw old lines (dimmer)
            for i, (line_words, _) in enumerate(visible_lines[-MAX_LINES:]):
                ly = DISPLAY_Y + i * LINE_HEIGHT
                draw_line(frame, line_words, ly, color=DIM_COLOR)

            # Draw prompt
            draw_line(frame, prompt, y)
            cursor_x = DISPLAY_X + prompt[0] + 1
            draw_cursor(frame, cursor_x, y)
            frames.append(frame)
            frames.append(frame.copy())  # Hold

            # Type each word
            typed = list(prompt)
            for word in rest:
                typed.append(word)
                frame = create_frame()

                # Draw old lines
                for i, (line_words, _) in enumerate(visible_lines[-MAX_LINES:]):
                    ly = DISPLAY_Y + i * LINE_HEIGHT
                    draw_line(frame, line_words, ly, color=DIM_COLOR)

                # Draw current typing
                draw_line(frame, typed, y)

                # Cursor after last word
                cursor_x = DISPLAY_X + sum(typed) + len(typed)
                draw_cursor(frame, cursor_x, y)

                frames.append(frame)

            # Final frame without cursor (command complete)
            frame = create_frame()
            for i, (line_words, _) in enumerate(visible_lines[-MAX_LINES:]):
                ly = DISPLAY_Y + i * LINE_HEIGHT
                draw_line(frame, line_words, ly, color=DIM_COLOR)
            draw_line(frame, words, y)
            frames.append(frame)
            frames.append(frame.copy())
            frames.append(frame.copy())  # Pause after command

            # Add to visible lines
            visible_lines.append((words, False))

        else:
            # Response: appears instantly
            visible_lines.append((words, False))

            # Mark older lines as old
            for i in range(len(visible_lines) - 1):
                visible_lines[i] = (visible_lines[i][0], True)

            frame = create_frame()
            display_lines = visible_lines[-MAX_LINES:]
            for i, (line_words, is_old) in enumerate(display_lines):
                ly = DISPLAY_Y + i * LINE_HEIGHT
                color = DIM_COLOR if is_old else DOT_COLOR
                draw_line(frame, line_words, ly, color=color)

            frames.append(frame)
            frames.append(frame.copy())  # Brief hold

    # Brief hold on final state then loop continues
    for _ in range(3):
        frames.append(frames[-1].copy())

    return frames

def main():
    frames = generate_console_animation()

    # Create the vertical strip texture
    total_height = len(frames) * FRAME_HEIGHT
    texture = Image.new('RGBA', (FRAME_WIDTH, total_height), BG_COLOR)

    for i, frame in enumerate(frames):
        texture.paste(frame, (0, i * FRAME_HEIGHT))

    # Save the texture
    output_path = 'src/main/resources/assets/logistics/textures/block/automation/laser_quarry/display.png'
    texture.save(output_path)
    print(f"Generated {len(frames)} frames ({FRAME_WIDTH}x{total_height} texture)")
    print(f"Saved to: {output_path}")

    # Update the mcmeta
    mcmeta_path = output_path + '.mcmeta'
    with open(mcmeta_path, 'w') as f:
        f.write('{\n\t"animation": {\n\t\t"frametime": 2\n\t}\n}\n')
    print(f"Updated: {mcmeta_path}")

if __name__ == '__main__':
    main()
