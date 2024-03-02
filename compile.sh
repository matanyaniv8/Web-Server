#!/bin/bash

src_dir="src"
out_dir="out_dir"

mkdir -p "$out_dir"

find "$src_dir" -name "*.java" -exec javac -d "$out_dir" {} +


echo "Compilation and copy completed."
