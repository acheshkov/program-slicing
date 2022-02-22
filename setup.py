from setuptools import setup


def parse_requirements_file(filename):
    with open(filename) as fid:
        requires = [line.strip() for line in fid.readlines() if not line.startswith("#")]

    return requires


install_requires = parse_requirements_file("requirements/default.txt")
extras_require = {}


packages = [
    'slicing',
    'slicing.block'
]

setup(
    name="program_slicing",
    description="Library to do Extract Method refactroting",
    version="0.1",
    packages=packages,
    author="Anton Cheshkov",
    python_requires=">=3.7",
    install_requires=install_requires,
    extras_require=extras_require,
    include_package_data=True,
    zip_safe=False,
    # cmdclass={"build_py": BuildCommand}
)
