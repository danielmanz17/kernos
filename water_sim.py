import pygame

pygame.init()

WIDTH = 300
HEIGHT = 600

screen = pygame.display.set_mode((WIDTH, HEIGHT))
clock = pygame.time.Clock()

flux = 0.0
alpha = 0.01

running = True

while running:

    clock.tick(60)

    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False

    keys = pygame.key.get_pressed()

    target = 1.0 if keys[pygame.K_SPACE] else 0.0

    # One-pole low-pass filter
    flux += (target - flux) * alpha

    # Drawing
    screen.fill((20, 20, 20))

    margin = 50
    bar_width = 100

    usable_height = HEIGHT - 2 * margin
    bar_height = int(flux * usable_height)

    x = (WIDTH - bar_width) // 2
    y = HEIGHT - margin - bar_height

    # Outline
    pygame.draw.rect(
        screen,
        (100, 100, 100),
        (x, margin, bar_width, usable_height),
        width=2,
    )

    # Filled column
    pygame.draw.rect(
        screen,
        (80, 180, 255),
        (x, y, bar_width, bar_height),
    )

    pygame.display.set_caption(
        f"Flux: {flux:.3f}    Target: {target:.1f}"
    )

    pygame.display.flip()

pygame.quit()