#!/bin/bash

# This script creates a new orphan branch in a Git repository.
# Usage: ./new-orphan.sh <branch-name>

show_help() {
    echo "Usage: $0 <branch-name>"
    echo "Creates a new orphan branch in a Git repository and cleans the working directory."
}

if [[ "$1" == "-h" || "$1" == "--help" || -z "$1" ]]; then
    show_help
    exit 0
fi

BRANCHNAME=$1

git checkout --orphan ${BRANCHNAME}
git reset HEAD -- # to unstage all files
git clean --dry-run # to see what will be removed

read -p "Do you want to proceed and remove these files? [y/N] " confirm
if [[ "$confirm" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    git clean -fd
    echo "Untracked files removed."
else
    echo "Cleanup aborted."
fi
