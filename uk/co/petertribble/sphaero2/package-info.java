/*
 * SPDX-License-Identifier: CDDL-1.0
 *
 * This file and its contents are supplied under the terms of the
 * Common Development and Distribution License ("CDDL"), version 1.0.
 * You may only use this file in accordance with the terms of version
 * 1.0 of the CDDL.
 *
 * A full copy of the text of the CDDL should have accompanied this
 * source. A copy of the CDDL is also available via the Internet at
 * http://www.illumos.org/license/CDDL.
 *
 * Copyright 2025 Peter C. Tribble
 */

/**
 * This package provides the sphaero2 application. The main user interface is
 * a JigsawFrame, while the actual puzzle itself is contained in JigsawPuzzle.
 * <p>
 * A puzzle is comprised of Pieces. The image is cut into Pieces by the
 * various Cutters, which are instances of JigsawCutter.
 */
package uk.co.petertribble.sphaero2;
