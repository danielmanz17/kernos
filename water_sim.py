import pygame
from pythonosc.udp_client import SimpleUDPClient
import math
import random

# persistent state
theta = 0.0
threshold = 0.3
prev_flux = 0.0

pygame.init()
client = SimpleUDPClient("127.0.0.1", 57120)

WIDTH = 300
HEIGHT = 600

screen = pygame.display.set_mode((WIDTH, HEIGHT))
clock = pygame.time.Clock()

flux = 0.0
alpha = 0.005

running = True

while running:

    clock.tick(60)

    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False

    keys = pygame.key.get_pressed()

    target = 1.0 if keys[pygame.K_SPACE] else 0.0

    # One-pole low-pass filter (0–1 domain)
    flux += (target - flux) * alpha

    # trigger: crossing threshold upward
    if flux > threshold and prev_flux <= threshold:
        theta = random.uniform(0, 2 * math.pi)
        print(f'new theta: {theta}')
        
    prev_flux = flux

    # polar mapping
    r = flux * 5.0  # radius scaling

    x_offset = r * math.cos(theta)
    y_offset = r * math.sin(theta)  # optional squash for aspect ratio

    # OSC output scaled to 0–5
    client.send_message("/flux", [x_offset, y_offset])

    # Drawing
    screen.fill((20, 20, 20))

    margin = 50
    bar_width = 100

    usable_height = HEIGHT - 2 * margin
    bar_height = int(flux * usable_height)

    x = (WIDTH - bar_width) // 2
    y = HEIGHT - margin - bar_height

    pygame.draw.rect(
        screen,
        (100, 100, 100),
        (x, margin, bar_width, usable_height),
        width=2,
    )

    pygame.draw.rect(
        screen,
        (80, 180, 255),
        (x, y, bar_width, bar_height),
    )

    pygame.display.set_caption(
        f"Flux: {flux:.3f} (0–1)   Target: {target:.1f}"
    )

    pygame.display.flip()

pygame.quit()