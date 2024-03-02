#!/bin/bash

out_dir="out_dir"

cd "$out_dir" || exit 1

java Main

echo "Execution completed."
